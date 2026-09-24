package learning.task043;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinTransferServiceTest {
    private Connection connection;

    @BeforeEach
    void prepareDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:task043_" + System.nanoTime() + ";MODE=PostgreSQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE player_wallets (player_id INTEGER PRIMARY KEY, coins INTEGER NOT NULL CHECK (coins BETWEEN 0 AND 100))");
            statement.execute("INSERT INTO player_wallets VALUES (1, 100), (2, 50), (3, 0)");
        }
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void transfersCoinsAndCommitsBothChanges() throws SQLException {
        assertTrue(new CoinTransferService(connection).transfer(1, 2, 30));
        assertBalances(70, 80, 0);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void allowsTransferOfEntireBalance() throws SQLException {
        assertTrue(new CoinTransferService(connection).transfer(1, 3, 100));
        assertBalances(0, 50, 100);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void returnsFalseWhenFundsAreInsufficient() throws SQLException {
        assertFalse(new CoinTransferService(connection).transfer(1, 2, 101));
        assertBalances(100, 50, 0);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void rollsBackWhenSenderDoesNotExist() throws SQLException {
        assertFalse(new CoinTransferService(connection).transfer(999, 2, 10));
        assertBalances(100, 50, 0);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void rollsBackWithdrawalWhenReceiverDoesNotExist() throws SQLException {
        assertFalse(new CoinTransferService(connection).transfer(1, 999, 10));
        assertBalances(100, 50, 0);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void rollsBackWithdrawalWhenReceiverRejectsDeposit() throws SQLException {
        assertThrows(SQLException.class, () -> new CoinTransferService(connection).transfer(1, 2, 60));
        assertBalances(100, 50, 0);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void rejectsInvalidArgumentsWithoutChangingWallets() throws SQLException {
        CoinTransferService service = new CoinTransferService(connection);
        assertThrows(IllegalArgumentException.class, () -> service.transfer(0, 2, 10));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(1, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(1, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(1, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(1, 2, -1));
        assertBalances(100, 50, 0);
        assertTrue(connection.getAutoCommit());
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(IllegalArgumentException.class, () -> new CoinTransferService(null));
    }

    @Test
    void doesNotCommitCallersExistingTransaction() throws SQLException {
        connection.setAutoCommit(false);
        assertThrows(IllegalStateException.class, () -> new CoinTransferService(connection).transfer(1, 2, 10));
        assertBalances(100, 50, 0);
        assertFalse(connection.getAutoCommit());
    }

    private void assertBalances(int first, int second, int third) throws SQLException {
        assertEquals(first, coins(1));
        assertEquals(second, coins(2));
        assertEquals(third, coins(3));
    }

    private int coins(int playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT coins FROM player_wallets WHERE player_id = ?")) {
            statement.setInt(1, playerId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt("coins");
            }
        }
    }
}
