package learning.task069;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RecruitsRepository {

    JdbcTemplate jdbcTemplate;
    public RecruitsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean registerRecruit(Recruit recruit) {
        try {
            String SQL = "INSERT INTO guild_recruits (id, name, level) VALUES (?, ?, ?)";
            int changed = jdbcTemplate.update(SQL, recruit.id(), recruit.name(), recruit.level());
            return changed == 1;
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException(e.getMessage());
        }
    }

    public Optional<Recruit> getRecruit(long id) {
        String SQL = "SELECT * FROM guild_recruits WHERE id = ?";
        return jdbcTemplate.query(SQL, (rs, rn) -> new Recruit(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("level")
        ), id).stream().findFirst();
    }

}
