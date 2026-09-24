package learning.task043;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CoinTransferService {
    private Connection connection;

    public CoinTransferService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException();
        }
        this.connection = connection;
    }

    public boolean transfer(int fromPlayerId, int toPlayerId, int amount) throws SQLException {
        if (fromPlayerId < 1 || toPlayerId < 1 || amount < 1 || fromPlayerId == toPlayerId)
            throw new IllegalArgumentException();
        if (!connection.getAutoCommit()) throw new IllegalStateException();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement debit = connection.prepareStatement("UPDATE player_wallets SET coins = coins - ? WHERE player_id = ? AND coins >= ?")) {
                debit.setInt(1, amount);
                debit.setInt(2, fromPlayerId);
                debit.setInt(3, amount);
                if (debit.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
            try (PreparedStatement credit = connection.prepareStatement("UPDATE player_wallets SET coins = coins + ? WHERE player_id = ?")) {
                credit.setInt(1, amount);
                credit.setInt(2, toPlayerId);
                if (credit.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
            connection.commit();
            return true;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
