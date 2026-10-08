package learning.task084;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trainer infrastructure: an isolated schema, optionally upgraded from a populated V1. */
final class PostgresTestDatabase implements AutoCloseable {
    static final int ORIGINAL_V1_CHECKSUM = -710521246;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String schema = "task084_" + UUID.randomUUID().toString().replace("-", "");
    private final AtomicBoolean closed = new AtomicBoolean();
    private List<List<Object>> legacyRows = List.of();
    private List<Map<String, Object>> v1History = List.of();
    private long originalTableOid;
    private String identitySequence;
    private List<List<Object>> sequenceState = List.of();

    PostgresTestDatabase() { this(true); }

    PostgresTestDatabase(boolean prepareLegacyDatabase) {
        baseUrl = required("COURSE_PG_URL");
        username = required("COURSE_PG_USER");
        password = System.getenv("COURSE_PG_PASSWORD");
        if (password == null) throw new IllegalStateException("Задай COURSE_PG_PASSWORD перед запуском тестов.");
        if (!baseUrl.startsWith("jdbc:postgresql://") || baseUrl.toLowerCase(Locale.ROOT).contains("currentschema")) {
            throw new IllegalStateException("Нужен JDBC URL PostgreSQL без currentSchema.");
        }
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            if (!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")) {
                throw new IllegalStateException("Тестам нужен настоящий PostgreSQL.");
            }
            statement.execute("CREATE SCHEMA " + schema);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось создать временную схему. Проверь подключение и право CREATE в учебной базе.", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { close(); } catch (Exception ignored) { }
        }, "task084-schema-cleanup"));
        if (prepareLegacyDatabase) {
            try {
                prepareV1WithExistingRows();
            } catch (Exception e) {
                try { close(); } catch (Exception cleanup) { e.addSuppressed(cleanup); }
                throw new IllegalStateException("Не удалось подготовить базу версии V1. Перенеси V1 из архива №83 без изменений.", e);
            }
        }
    }

    private void prepareV1WithExistingRows() throws SQLException {
        var flyway = Flyway.configure().dataSource(url(), username, password).target("1").load();
        flyway.migrate();
        var applied = flyway.info().applied();
        if (applied.length != 1 || !"1".equals(applied[0].getVersion().toString())
                || !Integer.valueOf(ORIGINAL_V1_CHECKSUM).equals(applied[0].getChecksum())) {
            throw new IllegalStateException("V1 должна совпадать с успешно применённой миграцией задачи №83.");
        }
        try (var connection = DriverManager.getConnection(url(), username, password)) {
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT * FROM recruits WHERE 1 = 0")) {
                if (rows.getMetaData().getColumnCount() != 3) {
                    throw new IllegalStateException("В V1 ещё нет coins; её добавляет только V2.");
                }
            }
            long first = insertLegacy(connection, " Рин \"Лис\" ", 0);
            long second = insertLegacy(connection, "Г".repeat(40), Integer.MAX_VALUE);
            long big = 3_000_000_000L;
            try (var insert = connection.prepareStatement("INSERT INTO recruits (id, name, level) VALUES (?, ?, ?)")) {
                insert.setLong(1, big); insert.setString(2, "Existing big ID"); insert.setInt(3, 7); insert.executeUpdate();
            }
            legacyRows = List.of(List.of(first, " Рин \"Лис\" ", 0),
                    List.of(second, "Г".repeat(40), Integer.MAX_VALUE), List.of(big, "Existing big ID", 7));
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT 'recruits'::regclass::oid")) {
                rows.next(); originalTableOid = rows.getLong(1);
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT pg_get_serial_sequence('recruits', 'id')")) {
                rows.next(); identitySequence = rows.getString(1);
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT last_value, is_called FROM " + identitySequence)) {
                rows.next(); sequenceState = List.of(List.of(rows.getLong("last_value"), rows.getBoolean("is_called")));
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT * FROM flyway_schema_history ORDER BY installed_rank")) {
                var history = new ArrayList<Map<String, Object>>();
                while (rows.next()) {
                    var entry = new LinkedHashMap<String, Object>();
                    for (int column = 1; column <= rows.getMetaData().getColumnCount(); column++) {
                        entry.put(rows.getMetaData().getColumnLabel(column), rows.getObject(column));
                    }
                    history.add(entry);
                }
                v1History = List.copyOf(history);
            }
        }
    }

    private long insertLegacy(Connection connection, String name, int level) throws SQLException {
        try (var insert = connection.prepareStatement("INSERT INTO recruits (name, level) VALUES (?, ?) RETURNING id")) {
            insert.setString(1, name); insert.setInt(2, level);
            try (var rows = insert.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Задай " + name + " перед запуском тестов.");
        return value;
    }

    String url() { return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema; }
    String username() { return username; }
    String password() { return password; }
    String schema() { return schema; }
    List<List<Object>> legacyRows() { return legacyRows; }
    List<Map<String, Object>> v1History() { return v1History; }
    long originalTableOid() { return originalTableOid; }
    String identitySequence() { return identitySequence; }
    List<List<Object>> sequenceState() { return sequenceState; }

    @Override
    public void close() throws SQLException {
        if (closed.compareAndSet(false, true)) {
            try (var connection = DriverManager.getConnection(baseUrl, username, password);
                 var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
