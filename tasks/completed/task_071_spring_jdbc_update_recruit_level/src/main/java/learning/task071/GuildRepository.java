package learning.task071;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GuildRepository {

    private final JdbcTemplate jdbcTemplate;
    public GuildRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int setLevel(RecruitBody recruitBody, long id) {
        return jdbcTemplate.update("UPDATE guild_recruits SET level = ? WHERE id = ?", recruitBody.level(), id);
    }
}
