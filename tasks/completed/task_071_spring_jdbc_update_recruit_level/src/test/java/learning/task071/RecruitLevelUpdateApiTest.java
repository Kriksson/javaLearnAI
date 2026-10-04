package learning.task071;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task071-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class RecruitLevelUpdateApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clearsRows() throws Exception {
        assertNotNull(dataSource, "Настрой H2 и Spring JDBC.");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM guild_recruits");
        }
    }

    @Test
    void createsJdbcAndRestComponentsAndRequiredSchema() throws Exception {
        assertFalse(applicationContext.getBeansWithAnnotation(Repository.class).isEmpty());
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty());
        assertNotNull(applicationContext.getBean(JdbcTemplate.class));
        assertNotNull(getClass().getClassLoader().getResource("schema.sql"),
                "Создай src/main/resources/schema.sql.");
        assertNotNull(getClass().getClassLoader().getResource("application.properties"),
                "Создай src/main/resources/application.properties.");
        assertEquals(List.of(), storedRows(), "Таблица должна создаваться пустой.");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, name, level FROM guild_recruits WHERE 1 = 0")) {
            ResultSetMetaData columns = rows.getMetaData();
            assertEquals(Types.BIGINT, columns.getColumnType(1));
            assertEquals("id", columns.getColumnName(1).toLowerCase(Locale.ROOT));
            assertTrue(columns.isAutoIncrement(1), "id должна генерировать база.");
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(1));
            assertEquals(Types.VARCHAR, columns.getColumnType(2));
            assertEquals(30, columns.getPrecision(2));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(2));
            assertEquals(Types.INTEGER, columns.getColumnType(3));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(3));
            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(
                    connection.getCatalog(), null, "GUILD_RECRUITS")) {
                while (keys.next()) {
                    primaryKeys.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            assertEquals(List.of("id"), primaryKeys);
        }
    }

    @ParameterizedTest
    @MethodSource("validLevels")
    void updatesLevelAndPreservesIdNameAndOtherRows(int newLevel) throws Exception {
        long targetId = insert("Лира", 25);
        long otherId = insert("О'Рин", 70);

        mockMvc.perform(put("/guild/recruits/{id}", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LevelRequest(newLevel))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertEquals(List.of(
                List.of(targetId, "Лира", newLevel),
                List.of(otherId, "О'Рин", 70)), storedRows());
    }

    private static Stream<Arguments> validLevels() {
        return Stream.of(Arguments.of(1), Arguments.of(42), Arguments.of(100));
    }

    @Test
    void acceptsIdLargerThanIntegerRange() throws Exception {
        restartIdentity(5_000_000_007L);
        long id = insert("Большой ID", 25);
        assertEquals(5_000_000_007L, id);

        mockMvc.perform(put("/guild/recruits/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":99}"))
                .andExpect(status().isNoContent());

        assertEquals(List.of(List.of(id, "Большой ID", 99)), storedRows());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "9223372036854775807", "-9223372036854775808"})
    void returnsNotFoundForMissingLongIdWithoutInserting(String missingId) throws Exception {
        long existingId = insert("Сохранённый", 25);
        List<List<Object>> before = storedRows();

        mockMvc.perform(put("/guild/recruits/{id}", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":80}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        assertEquals(List.of(List.of(existingId, "Сохранённый", 25)), before);
        assertEquals(before, storedRows());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "-9223372036854775809"})
    void returnsBadRequestForInvalidPathIdWithoutChangingRows(String invalidId) throws Exception {
        insert("Сохранённый", 25);
        List<List<Object>> before = storedRows();

        mockMvc.perform(put("/guild/recruits/{id}", invalidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":80}"))
                .andExpect(status().isBadRequest());

        assertEquals(before, storedRows());
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void returnsBadRequestForInvalidBodyWithoutChangingRows(String body) throws Exception {
        insert("Сохранённый", 25);
        List<List<Object>> before = storedRows();

        mockMvc.perform(put("/guild/recruits/{id}", before.getFirst().getFirst())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertEquals(before, storedRows());
    }

    private static Stream<String> invalidBodies() {
        return Stream.of(
                "{}",
                "{\"level\":null}",
                "{\"level\":0}",
                "{\"level\":-1}",
                "{\"level\":101}",
                "{\"level\":2147483648}",
                "{\"level\":\"abc\"}",
                "[]",
                "",
                "{\"level\":"
        );
    }

    @Test
    void repeatingSamePutIsIdempotent() throws Exception {
        long id = insert("Лира", 25);
        byte[] body = objectMapper.writeValueAsBytes(new LevelRequest(45));

        mockMvc.perform(put("/guild/recruits/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        List<List<Object>> afterFirstRequest = storedRows();
        mockMvc.perform(put("/guild/recruits/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        assertEquals(afterFirstRequest, storedRows());
        assertEquals(List.of(List.of(id, "Лира", 45)), afterFirstRequest);
    }

    private long insert(String name, int level) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_recruits (name, level) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, name);
            insert.setInt(2, level);
            assertEquals(1, insert.executeUpdate());
            try (ResultSet keys = insert.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private void restartIdentity(long nextId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE guild_recruits ALTER COLUMN id RESTART WITH " + nextId);
        }
    }

    private List<List<Object>> storedRows() throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT id, name, level FROM guild_recruits ORDER BY id")) {
            while (result.next()) {
                rows.add(List.of(result.getLong("id"), result.getString("name"), result.getInt("level")));
            }
        }
        return rows;
    }

    private record LevelRequest(int level) {
    }
}
