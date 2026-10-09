package learning.task088;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Infrastructure owned by the tests: every run uses its own PostgreSQL schema. */
final class PostgresTestDatabase implements AutoCloseable {
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String schema = "task088_" + UUID.randomUUID().toString().replace("-", "");
    private final AtomicBoolean closed = new AtomicBoolean();

    PostgresTestDatabase() {
        baseUrl = required("COURSE_PG_URL");
        username = required("COURSE_PG_USER");
        password = System.getenv("COURSE_PG_PASSWORD");
        if (password == null) throw new IllegalStateException("Задай COURSE_PG_PASSWORD перед запуском тестов.");
        if (!baseUrl.startsWith("jdbc:postgresql://") || baseUrl.toLowerCase().contains("currentschema")) {
            throw new IllegalStateException("COURSE_PG_URL должен быть JDBC URL PostgreSQL без currentSchema.");
        }
        try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            if (!connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")) {
                throw new IllegalStateException("Тестам нужен настоящий PostgreSQL.");
            }
            statement.execute("CREATE SCHEMA " + schema);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось создать отдельную схему тестов. Проверь PostgreSQL, драйвер, подключение и право CREATE в учебной базе.", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { close(); } catch (Exception ignored) { }
        }, "task088-schema-cleanup"));
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

    @Override
    public void close() throws SQLException {
        if (closed.compareAndSet(false, true)) {
            try (Connection connection = DriverManager.getConnection(baseUrl, username, password);
                 var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
