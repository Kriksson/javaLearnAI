package learning.task036;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerOrdersSchemaTest {
    private Connection connection;

    @BeforeEach
    void createSchema() throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task036_" + System.nanoTime() + ";MODE=PostgreSQL");
        executeScript(readSchema());
    }

    @Test
    void createsOrderForExistingPlayer() throws SQLException {
        int playerId = insertGeneratedPlayer("Kira", 42);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO player_orders (player_id, item_name, total_price, purchased_at) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, playerId);
            statement.setString(2, "Magic sword");
            statement.setBigDecimal(3, new java.math.BigDecimal("199.99"));
            statement.setDate(4, Date.valueOf("2026-09-23"));
            assertEquals(1, statement.executeUpdate());
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertTrue(generatedKeys.next());
            }
        }

        try (ResultSet result = connection.createStatement().executeQuery(
                "SELECT player_id, item_name, total_price, purchased_at FROM player_orders")) {
            assertTrue(result.next());
            assertEquals(playerId, result.getInt("player_id"));
            assertEquals("Magic sword", result.getString("item_name"));
            assertEquals(new java.math.BigDecimal("199.99"), result.getBigDecimal("total_price"));
            assertEquals(Date.valueOf("2026-09-23"), result.getDate("purchased_at"));
        }
    }

    @Test
    void rejectsDuplicateNicknameAndInvalidLevel() throws SQLException {
        insertPlayer(1, "Kira", 42);
        assertThrows(SQLException.class, () -> insertPlayer(2, "Kira", 10));
        assertThrows(SQLException.class, () -> insertPlayer(3, "Milo", 0));
        assertThrows(SQLException.class, () -> insertPlayer(4, "Nova", 101));
    }

    @Test
    void rejectsOrderWithoutPlayerAndNegativePrice() throws SQLException {
        insertPlayer(1, "Kira", 42);
        assertThrows(SQLException.class, () -> insertOrder(10, 999, "Potion", "10.00"));
        assertThrows(SQLException.class, () -> insertOrder(11, 1, "Potion", "-0.01"));
    }

    private void insertPlayer(int id, String nickname, int level) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO players (id, nickname, level) VALUES (?, ?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, nickname);
            statement.setInt(3, level);
            statement.executeUpdate();
        }
    }

    private int insertGeneratedPlayer(String nickname, int level) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO players (nickname, level) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, nickname);
            statement.setInt(2, level);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertTrue(generatedKeys.next());
                return generatedKeys.getInt(1);
            }
        }
    }

    private void insertOrder(int id, int playerId, String itemName, String price) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO player_orders (id, player_id, item_name, total_price, purchased_at) VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, id);
            statement.setInt(2, playerId);
            statement.setString(3, itemName);
            statement.setBigDecimal(4, new java.math.BigDecimal(price));
            statement.setDate(5, Date.valueOf("2026-09-23"));
            statement.executeUpdate();
        }
    }

    private void executeScript(String script) throws SQLException {
        for (String statement : script.replaceAll("(?m)^\\s*--.*$", "").split(";")) {
            if (!statement.isBlank()) {
                connection.createStatement().execute(statement);
            }
        }
    }

    private String readSchema() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/learning/task036/schema.sql")) {
            if (input == null) {
                throw new IOException("Не найден schema.sql");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
