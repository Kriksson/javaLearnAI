package learning.task072;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GuildRepository {

    private final JdbcTemplate jdbcTemplate;
    public GuildRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int take(long id) {
        return jdbcTemplate.update("UPDATE guild_chests SET coins = coins - 1 WHERE id = ? AND coins >= 1", id);
    }

    public int put(long id) {
        return jdbcTemplate.update("UPDATE guild_chests SET coins = coins + 1 WHERE id = ?", id);
    }

}
