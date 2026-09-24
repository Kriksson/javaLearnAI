package learning.task037;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerOrderReportsTest {
    private Connection connection;
    private List<String> queries;

    @BeforeEach
    void prepareDatabase() throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task037_" + System.nanoTime() + ";MODE=PostgreSQL");
        createTablesAndData();
        queries = readQueries();
        assertEquals(3, queries.size(), "В queries.sql должны быть ровно три SQL-команды, разделённые ;");
    }

    @Test
    void selectsHighLevelPlayersInRequiredOrder() throws SQLException {
        try (ResultSet result = executeQuery(0)) {
            assertEquals(List.of("Nova:100", "Kira:42"), rows(result, "nickname", "level"));
        }
    }

    @Test
    void selectsFirstPlayerOrdersInRequiredOrder() throws SQLException {
        try (ResultSet result = executeQuery(1)) {
            assertEquals(List.of("Magic sword:199.99", "Bow:50.00"), rows(result, "item_name", "total_price"));
        }
    }

    @Test
    void sumsOrdersByPlayerAndUsesRequiredAlias() throws SQLException {
        try (ResultSet result = executeQuery(2)) {
            assertEquals(List.of("1:249.99", "3:200.00", "2:20.00"), rows(result, "player_id", "total_spent"));
        }
    }

    private ResultSet executeQuery(int index) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            return statement.executeQuery(queries.get(index));
        } catch (SQLException exception) {
            statement.close();
            throw exception;
        }
    }

    private List<String> rows(ResultSet result, String firstColumn, String secondColumn) throws SQLException {
        List<String> rows = new ArrayList<>();
        while (result.next()) {
            Object value = result.getObject(secondColumn);
            rows.add(result.getObject(firstColumn) + ":" + (value instanceof BigDecimal decimal ? decimal.toPlainString() : value));
        }
        return rows;
    }

    private List<String> readQueries() throws IOException {
        String content;
        try (InputStream input = getClass().getResourceAsStream("/learning/task037/queries.sql")) {
            if (input == null) {
                throw new IOException("Не найден queries.sql");
            }
            content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<String> statements = new ArrayList<>();
        for (String statement : content.replaceAll("(?m)^\\s*--.*$", "").split(";")) {
            if (!statement.isBlank()) {
                statements.add(statement.trim());
            }
        }
        return statements;
    }

    private void createTablesAndData() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players (id INTEGER PRIMARY KEY, nickname VARCHAR NOT NULL UNIQUE, level INTEGER NOT NULL CHECK (level BETWEEN 1 AND 100))");
            statement.execute("CREATE TABLE player_orders (id INTEGER PRIMARY KEY, player_id INTEGER NOT NULL REFERENCES players(id), item_name VARCHAR NOT NULL, total_price DECIMAL(10, 2) NOT NULL CHECK (total_price >= 0), purchased_at DATE NOT NULL)");
            statement.execute("INSERT INTO players VALUES (1, 'Kira', 42), (2, 'Milo', 7), (3, 'Nova', 100)");
            statement.execute("INSERT INTO player_orders VALUES (1, 1, 'Magic sword', 199.99, DATE '2026-09-23'), (2, 1, 'Bow', 50.00, DATE '2026-09-23'), (3, 2, 'Shield', 20.00, DATE '2026-09-23'), (4, 3, 'Dragon armor', 200.00, DATE '2026-09-23')");
        }
    }
}
