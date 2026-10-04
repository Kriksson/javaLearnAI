package learning.task069;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task069-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecruitRegistrationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DataSource dataSource;

    @BeforeAll
    void checksStartupTableAndResources() throws SQLException {
        assertNotNull(dataSource, "Настрой H2 и Spring JDBC.");
        assertNotNull(getClass().getClassLoader().getResource("schema.sql"),
                "Создай src/main/resources/schema.sql.");
        assertNotNull(getClass().getClassLoader().getResource("application.properties"),
                "Создай src/main/resources/application.properties.");
        assertEquals(List.of(), storedRows(), "При старте таблица должна быть пустой.");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, name, level FROM guild_recruits")) {
            ResultSetMetaData columns = rows.getMetaData();
            assertEquals(Types.BIGINT, columns.getColumnType(1));
            assertEquals(Types.VARCHAR, columns.getColumnType(2));
            assertEquals(30, columns.getPrecision(2), "Для имени нужен VARCHAR(30).");
            assertEquals(Types.INTEGER, columns.getColumnType(3));
            for (int i = 1; i <= 3; i++) {
                assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(i),
                        "Колонки id, name и level должны запрещать NULL.");
            }
            List<String> primaryKey = new ArrayList<>();
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(
                    connection.getCatalog(), null, "GUILD_RECRUITS")) {
                while (keys.next()) {
                    primaryKey.add(keys.getString("COLUMN_NAME").toLowerCase(java.util.Locale.ROOT));
                }
            }
            assertEquals(List.of("id"), primaryKey, "Первичный ключ таблицы — id.");
        }
    }

    @BeforeEach
    void clearsRows() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM guild_recruits");
        }
    }

    @Test
    void registersSpringComponentsAndJdbcTemplate() throws Exception {
        assertFalse(applicationContext.getBeansWithAnnotation(Repository.class).isEmpty());
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty());
        assertNotNull(applicationContext.getBean(Class.forName("org.springframework.jdbc.core.JdbcTemplate")));
    }

    @ParameterizedTest
    @MethodSource("validRecruits")
    void createsExactlyOneRowAndReadsIt(long id, String name, int level) throws Exception {
        MvcResult result = mockMvc.perform(post("/guild/recruits")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(id, name, level)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/guild/recruits/" + id))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        assertRecruit(result, id, name, level);
        assertEquals(List.of(List.of(id, name, level)), storedRows());
        expectRecruit(id, name, level);
    }

    private static Stream<Arguments> validRecruits() {
        return Stream.of(
                Arguments.of(40L, "Лира", 25),
                Arguments.of(1L, "А", 1),
                Arguments.of(Long.MAX_VALUE, "Б".repeat(30), 100),
                Arguments.of(5_000_000_007L, "Новичок", 42),
                Arguments.of(80L, " О'Рин \"Маг\" ", 70)
        );
    }

    @Test
    void allowsSameNameForDifferentIdsAndKeepsBothRows() throws Exception {
        create(40, "Лира", 25);
        create(50, "Лира", 100);
        expectRecruit(40, "Лира", 25);
        expectRecruit(50, "Лира", 100);
        assertEquals(List.of(List.of(40L, "Лира", 25), List.of(50L, "Лира", 100)), storedRows());
    }

    @ParameterizedTest
    @MethodSource("duplicateRecruits")
    void duplicateIdReturnsConflictAndPreservesAllRows(String name, int level) throws Exception {
        create(40, "Лира", 25);
        insertDirectly(50, "Другой", 1);
        List<List<Object>> before = storedRows();
        mockMvc.perform(post("/guild/recruits")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(40, name, level)))
                .andExpect(status().isConflict());
        assertEquals(before, storedRows());
        expectRecruit(40, "Лира", 25);
    }

    private static Stream<Arguments> duplicateRecruits() {
        return Stream.of(Arguments.of("Лира", 25), Arguments.of("Мира", 100));
    }

    @Test
    void duplicateIdInsertedThroughAnotherConnectionAlsoReturnsConflict() throws Exception {
        insertDirectly(90, "Из базы", 77);
        List<List<Object>> before = storedRows();
        mockMvc.perform(post("/guild/recruits")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(90, "Новый", 10)))
                .andExpect(status().isConflict());
        assertEquals(before, storedRows());
        expectRecruit(90, "Из базы", 77);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidPostReturnsBadRequestWithoutChangingDatabase(String json) throws Exception {
        insertDirectly(10, "Существующий", 42);
        List<List<Object>> before = storedRows();
        mockMvc.perform(post("/guild/recruits").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        assertEquals(before, storedRows());
    }

    private static Stream<String> invalidRequests() {
        return Stream.of(
                "{\"name\":\"Лира\",\"level\":25}",
                "{\"id\":null,\"name\":\"Лира\",\"level\":25}",
                "{\"id\":0,\"name\":\"Лира\",\"level\":25}",
                "{\"id\":-1,\"name\":\"Лира\",\"level\":25}",
                "{\"id\":-9223372036854775808,\"name\":\"Лира\",\"level\":25}",
                "{\"id\":9223372036854775808,\"name\":\"Лира\",\"level\":25}",
                "{\"id\":\"abc\",\"name\":\"Лира\",\"level\":25}",
                "{\"id\":40,\"level\":25}",
                "{\"id\":40,\"name\":null,\"level\":25}",
                "{\"id\":40,\"name\":\"\",\"level\":25}",
                "{\"id\":40,\"name\":\"   \",\"level\":25}",
                "{\"id\":40,\"name\":\"\\t\\n\",\"level\":25}",
                "{\"id\":40,\"name\":\"" + "А".repeat(31) + "\",\"level\":25}",
                "{\"id\":40,\"name\":\"Лира\"}",
                "{\"id\":40,\"name\":\"Лира\",\"level\":null}",
                "{\"id\":40,\"name\":\"Лира\",\"level\":0}",
                "{\"id\":40,\"name\":\"Лира\",\"level\":-1}",
                "{\"id\":40,\"name\":\"Лира\",\"level\":101}",
                "{\"id\":40,\"name\":\"Лира\",\"level\":2147483648}",
                "{\"id\":40,\"name\":\"Лира\",\"level\":\"abc\"}",
                "{}", "", "null", "[]", "{\"id\":40,"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "999", "9223372036854775807", "-9223372036854775808"})
    void missingLongIdReturnsNotFoundWithoutChangingDatabase(String id) throws Exception {
        insertDirectly(10, "Лира", 25);
        List<List<Object>> before = storedRows();
        mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        assertEquals(before, storedRows());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "-9223372036854775809"})
    void invalidGetIdReturnsBadRequestWithoutChangingDatabase(String id) throws Exception {
        insertDirectly(10, "Лира", 25);
        List<List<Object>> before = storedRows();
        mockMvc.perform(get("/guild/recruits/{id}", id)).andExpect(status().isBadRequest());
        assertEquals(before, storedRows());
    }

    @Test
    void readsInsertedUpdatedAndDeletedRowsThroughAnotherConnection() throws Exception {
        insertDirectly(5_000_000_007L, "Из базы", 1);
        expectRecruit(5_000_000_007L, "Из базы", 1);
        create(40, "Лира", 25);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE guild_recruits SET name = ?, level = ? WHERE id = ?")) {
            update.setString(1, " Мира \"Свет\" ");
            update.setInt(2, 100);
            update.setLong(3, 40);
            assertEquals(1, update.executeUpdate());
        }
        expectRecruit(40, " Мира \"Свет\" ", 100);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement delete = connection.prepareStatement("DELETE FROM guild_recruits WHERE id = ?")) {
            delete.setLong(1, 40);
            assertEquals(1, delete.executeUpdate());
        }
        mockMvc.perform(get("/guild/recruits/40"))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
        assertEquals(List.of(List.of(5_000_000_007L, "Из базы", 1)), storedRows());
    }

    @Test
    void repeatedGetDoesNotChangeStoredRows() throws Exception {
        create(40, "Лира", 25);
        insertDirectly(50, "Мира", 100);
        List<List<Object>> before = storedRows();
        expectRecruit(40, "Лира", 25);
        expectRecruit(40, "Лира", 25);
        assertEquals(before, storedRows());
    }

    @Test
    void primaryKeyRejectsDuplicateInsertedDirectly() throws Exception {
        insertDirectly(40, "Лира", 25);
        assertThrows(SQLException.class, () -> insertDirectly(40, "Другой", 100));
        assertEquals(List.of(List.of(40L, "Лира", 25)), storedRows());
    }

    private void create(long id, String name, int level) throws Exception {
        MvcResult result = mockMvc.perform(post("/guild/recruits")
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson(id, name, level)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/guild/recruits/" + id))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();
        assertRecruit(result, id, name, level);
    }

    private void expectRecruit(long id, String name, int level) throws Exception {
        MvcResult result = mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();
        assertRecruit(result, id, name, level);
    }

    private void assertRecruit(MvcResult result, long id, String name, int level) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(response.path("id").isIntegralNumber(), "id должен быть JSON-числом.");
        assertEquals(id, response.path("id").asLong());
        assertTrue(response.path("name").isTextual(), "name должен быть JSON-строкой.");
        assertEquals(name, response.path("name").asText());
        assertTrue(response.path("level").isIntegralNumber(), "level должен быть JSON-числом.");
        assertEquals(level, response.path("level").asInt());
    }

    private String requestJson(long id, String name, int level) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("id", id).put("name", name).put("level", level));
    }

    private void insertDirectly(long id, String name, int level) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_recruits (id, name, level) VALUES (?, ?, ?)")) {
            insert.setLong(1, id);
            insert.setString(2, name);
            insert.setInt(3, level);
            assertEquals(1, insert.executeUpdate());
        }
    }

    private List<List<Object>> storedRows() throws SQLException {
        List<List<Object>> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, name, level FROM guild_recruits ORDER BY id")) {
            while (rows.next()) {
                result.add(List.of(rows.getLong("id"), rows.getString("name"), rows.getInt("level")));
            }
        }
        return result;
    }
}
