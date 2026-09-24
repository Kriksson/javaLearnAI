package learning.task040;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStateChangesTest {
    private Connection connection;
    private List<String> commands;

    @BeforeEach
    void prepareDatabase() throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task040_" + System.nanoTime() + ";MODE=PostgreSQL");
        createTableAndData();
        commands = readCommands();
        assertEquals(2, commands.size(), "В changes.sql должны быть ровно две SQL-команды");
    }

    @Test
    void updatesOnlyPaidOrderAndDeletesCancelledOrders() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            assertEquals(1, statement.executeUpdate(commands.get(0)), "Должен обновиться только заказ 101");
            assertEquals(0, statement.executeUpdate(commands.get(0)), "Оплаченный заказ нельзя повторно обновить из состояния PENDING");
            assertEquals(2, statement.executeUpdate(commands.get(1)), "Должны удалиться только два отменённых заказа");
        }

        Map<Integer, String> actualStatuses = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT id, status FROM player_orders")) {
            while (result.next()) {
                actualStatuses.put(result.getInt("id"), result.getString("status"));
            }
        }

        assertEquals(Map.of(101, "PAID", 102, "PAID", 104, "PENDING"), actualStatuses);
    }

    private List<String> readCommands() throws IOException {
        String content;
        try (InputStream input = getClass().getResourceAsStream("/learning/task040/changes.sql")) {
            if (input == null) {
                throw new IOException("Не найден changes.sql");
            }
            content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<String> result = new ArrayList<>();
        for (String command : content.replaceAll("(?m)^\\s*--.*$", "").split(";")) {
            if (!command.isBlank()) {
                result.add(command.trim());
            }
        }
        return result;
    }

    private void createTableAndData() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE player_orders (id INTEGER PRIMARY KEY, player_id INTEGER NOT NULL, item_name VARCHAR NOT NULL, status VARCHAR NOT NULL CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')))");
            statement.execute("INSERT INTO player_orders VALUES (101, 1, 'Magic sword', 'PENDING'), (102, 1, 'Bow', 'PAID'), (103, 2, 'Potion', 'CANCELLED'), (104, 2, 'Shield', 'PENDING'), (105, 3, 'Dragon armor', 'CANCELLED')");
        }
    }
}
