package learning.task041;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PlayerRepository {
    private Connection connection;

    public PlayerRepository(Connection connection) {
        if (connection == null) throw new IllegalArgumentException();
        this.connection = connection;
    }

    public Optional<Player> findByNickname(String nickname) throws SQLException {
        if (nickname == null || nickname.trim().isEmpty()) throw new IllegalArgumentException();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM players WHERE nickname = ?")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                int id = rs.getInt("id");
                String nick = rs.getString("nickname");
                int level = rs.getInt("level");

                return Optional.of(new Player(id, nick, level));
            }
        }
    }
}
