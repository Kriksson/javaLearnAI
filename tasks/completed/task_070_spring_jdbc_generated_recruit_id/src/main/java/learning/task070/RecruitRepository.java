package learning.task070;


import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class RecruitRepository {

    private final JdbcTemplate jdbcTemplate;
    public RecruitRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long add(Recruit recruit) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(con -> {
                PreparedStatement statement = con.prepareStatement("INSERT INTO guild_recruits (name, level) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, recruit.name());
                statement.setInt(2, recruit.level());
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("Key is null");
            }
            return key.longValue();
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Recruit already exists");
        }
    }

    public Optional<Recruit> findById(long id) {
        String SQL = "SELECT * FROM guild_recruits WHERE id = ?";
        return jdbcTemplate.query(SQL, (rs, rn) -> new Recruit(
                rs.getString("name"),
                rs.getInt("level"))
                , id).stream().findFirst();
    }
}
