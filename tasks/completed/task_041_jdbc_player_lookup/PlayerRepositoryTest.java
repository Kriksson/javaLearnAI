package learning.task041;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerRepositoryTest {
    private Connection connection;

    @BeforeEach
    void prepareDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task041_" + System.nanoTime() + ";MODE=PostgreSQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE players (id INTEGER PRIMARY KEY, nickname VARCHAR(100) NOT NULL UNIQUE, level INTEGER NOT NULL)");
            statement.execute("INSERT INTO players VALUES (1, 'Kira', 42), (2, 'O''Neil', 17)");
        }
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void findsPlayerByNickname() throws SQLException {
        PlayerRepository repository = new PlayerRepository(connection);
        assertEquals(Optional.of(new Player(1, "Kira", 42)), repository.findByNickname("Kira"));
    }

    @Test
    void findsNicknameContainingApostrophe() throws SQLException {
        PlayerRepository repository = new PlayerRepository(connection);
        assertEquals(Optional.of(new Player(2, "O'Neil", 17)), repository.findByNickname("O'Neil"));
    }

    @Test
    void returnsEmptyForUnknownAndNonExactNickname() throws SQLException {
        PlayerRepository repository = new PlayerRepository(connection);
        assertEquals(Optional.empty(), repository.findByNickname("Unknown"));
        assertEquals(Optional.empty(), repository.findByNickname("kira"));
        assertEquals(Optional.empty(), repository.findByNickname(" Kira "));
    }

    @Test
    void rejectsInvalidNicknames() {
        PlayerRepository repository = new PlayerRepository(connection);
        assertThrows(IllegalArgumentException.class, () -> repository.findByNickname(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findByNickname(""));
        assertThrows(IllegalArgumentException.class, () -> repository.findByNickname("   "));
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerRepository(null));
    }

    @Test
    void propagatesDatabaseError() throws SQLException {
        PlayerRepository repository = new PlayerRepository(connection);
        connection.close();
        assertThrows(SQLException.class, () -> repository.findByNickname("Kira"));
    }

    @Test
    void keepsCallerConnectionOpen() throws SQLException {
        PlayerRepository repository = new PlayerRepository(connection);
        repository.findByNickname("Kira");
        assertFalse(connection.isClosed());
    }
}
