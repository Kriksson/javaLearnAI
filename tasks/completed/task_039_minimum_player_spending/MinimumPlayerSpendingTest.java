package learning.task039;

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

class MinimumPlayerSpendingTest {
    private Connection connection;
    private String query;

    @BeforeEach
    void prepareDatabase() throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task039_" + System.nanoTime() + ";MODE=PostgreSQL");
        createTablesAndData();
        query = readQuery();
    }

    @Test
    void reportsPlayersAtOrAboveThresholdInRequiredOrder() throws SQLException {
        List<String> actual = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                BigDecimal spent = result.getBigDecimal("total_spent");
                actual.add(result.getString("nickname") + ":" + spent.toPlainString());
            }
        }

        assertEquals(List.of("Nova:150.00", "Kira:100.00", "Milo:100.00"), actual);
    }

    private String readQuery() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/learning/task039/query.sql")) {
            if (input == null) {
                throw new IOException("Не найден query.sql");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^\\s*--.*$", "").trim();
        }
    }

    private void createTablesAndData() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players (id INTEGER PRIMARY KEY, nickname VARCHAR NOT NULL UNIQUE)");
            statement.execute("CREATE TABLE player_orders (id INTEGER PRIMARY KEY, player_id INTEGER NOT NULL REFERENCES players(id), total_price DECIMAL(10, 2) NOT NULL CHECK (total_price >= 0))");
            statement.execute("INSERT INTO players VALUES (2, 'Nova'), (3, 'Milo'), (1, 'Zed'), (10, 'Kira'), (4, 'Luna')");
            statement.execute("INSERT INTO player_orders VALUES (1, 2, 120.00), (2, 2, 30.00), (3, 3, 90.00), (4, 3, 10.00), (5, 10, 70.00), (6, 10, 30.00), (7, 4, 99.99)");
        }
    }
}
