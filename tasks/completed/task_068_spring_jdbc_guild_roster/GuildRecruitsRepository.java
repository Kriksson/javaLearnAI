package learning.task068;


import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

@Repository
public class GuildRecruitsRepository {

    private final JdbcTemplate jdbc;

    public GuildRecruitsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Recruit> getRecruitById(long id) {
        String sql = "SELECT * FROM guild_recruits WHERE id = ?";

        return jdbc.query(sql, (rs, rn) -> new Recruit(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("level")
        ), id).stream().findFirst();
    }
}
