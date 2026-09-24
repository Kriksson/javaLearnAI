package learning.task042;

import java.sql.*;

public class PlayerRepository {
    private Connection connection;

    public PlayerRepository(Connection connection) {
        if (connection == null) throw new IllegalArgumentException();
        this.connection = connection;
    }

    public Player save(String nickname, int level) throws SQLException {
        if (nickname == null) throw new IllegalArgumentException();
        if (nickname.trim().isEmpty() || level < 1 || level > 100) throw new IllegalArgumentException();
        try (PreparedStatement ps =  connection.prepareStatement("INSERT INTO players (nickname, level) " +
                "VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nickname);
            ps.setInt(2, level);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException();

                int id = rs.getInt(1);
                return new Player(id, nickname, level);
            }
        }
    }
}
