package learning.task038;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerOrderCountsTest {
    private Connection connection;
    private String query;

    @BeforeEach
    void prepareDatabase() throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task038_" + System.nanoTime() + ";MODE=PostgreSQL");
        createTablesAndData();
        query = readQuery();
    }

    @Test
    void countsOrdersForEveryPlayerIncludingPlayersWithoutOrders() throws SQLException {
        List<String> actual = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                actual.add(result.getString("nickname") + ":" + result.getInt("order_count"));
            }
        }

        assertEquals(List.of("Kira:2", "Milo:1", "Nova:1", "Zed:0"), actual);
    }

    private String readQuery() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/learning/task038/query.sql")) {
            if (input == null) {
                throw new IOException("Не найден query.sql");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("(?m)^\\s*--.*$", "").trim();
        }
    }

    private void createTablesAndData() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players (id INTEGER PRIMARY KEY, nickname VARCHAR NOT NULL UNIQUE, level INTEGER NOT NULL)");
            statement.execute("CREATE TABLE player_orders (id INTEGER PRIMARY KEY, player_id INTEGER NOT NULL REFERENCES players(id), item_name VARCHAR NOT NULL)");
            statement.execute("INSERT INTO players VALUES (1, 'Kira', 42), (2, 'Milo', 7), (3, 'Nova', 100), (4, 'Zed', 15)");
            statement.execute("INSERT INTO player_orders VALUES (1, 1, 'Magic sword'), (2, 1, 'Bow'), (3, 2, 'Shield'), (4, 3, 'Dragon armor')");
        }
    }
}
