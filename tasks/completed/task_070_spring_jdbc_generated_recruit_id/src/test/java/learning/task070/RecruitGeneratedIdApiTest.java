package learning.task070;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task070-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecruitGeneratedIdApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DataSource dataSource;

    @BeforeAll
    void checksIdentitySchemaAndEmptyStart() throws Exception {
        assertNotNull(dataSource, "Настрой H2 и Spring JDBC.");
        assertNotNull(getClass().getClassLoader().getResource("schema.sql"),
                "Создай src/main/resources/schema.sql.");
        assertNotNull(getClass().getClassLoader().getResource("application.properties"),
                "Создай src/main/resources/application.properties.");
        assertEquals(List.of(), storedRows(), "Таблица должна создаваться пустой.");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, name, level FROM guild_recruits WHERE 1 = 0")) {
            ResultSetMetaData columns = rows.getMetaData();
            assertEquals(Types.BIGINT, columns.getColumnType(1));
            assertEquals("id", columns.getColumnName(1).toLowerCase(Locale.ROOT));
            assertTrue(columns.isAutoIncrement(1), "База должна выдавать id через identity.");
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(1));
            assertEquals(Types.VARCHAR, columns.getColumnType(2));
            assertEquals(30, columns.getPrecision(2), "Для name нужен VARCHAR(30).");
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
            assertEquals(List.of("id"), primaryKeys, "Первичный ключ — генерируемый id.");
        }
    }

    @BeforeEach
    void clearsRows() throws Exception {
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
    void createsOneRecruitGetsDatabaseIdAndReadsIt(String name, int level) throws Exception {
        long id = create(name, level);
        assertEquals(List.of(List.of(id, name, level)), storedRows());
        expectRecruit(id, name, level);
    }

    private static Stream<Arguments> validRecruits() {
        return Stream.of(
                Arguments.of("Лира", 25),
                Arguments.of("А", 1),
                Arguments.of("Б".repeat(30), 100),
                Arguments.of(" О'Рин \"Маг\" ", 70)
        );
    }

    @Test
    void sameNameCanBelongToTwoGeneratedIds() throws Exception {
        long first = create("Лира", 25);
        long second = create("Лира", 100);
        assertTrue(first > 0);
        assertTrue(second > 0);
        assertTrue(first != second, "ID каждой строки выдаёт база.");
        assertEquals(List.of(List.of(first, "Лира", 25), List.of(second, "Лира", 100)), storedRows());
    }

    @Test
    void readsIdGeneratedAfterApplicationStartupAndUpdatedRow() throws Exception {
        restartIdentity(5_000_000_007L);
        long id = create("Новичок", 42);
        assertEquals(5_000_000_007L, id, "Получай фактический большой BIGINT от базы.");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE guild_recruits SET name = ?, level = ? WHERE id = ?")) {
            update.setString(1, " Новое \"Имя\" ");
            update.setInt(2, 100);
            update.setLong(3, id);
            assertEquals(1, update.executeUpdate());
        }
        expectRecruit(id, " Новое \"Имя\" ", 100);
        assertEquals(List.of(List.of(id, " Новое \"Имя\" ", 100)), storedRows());
    }

    @Test
    void readsExternallyInsertedGeneratedId() throws Exception {
        long id = insertDirectly("Из базы", 77);
        assertTrue(id > 0);
        expectRecruit(id, "Из базы", 77);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidPostReturnsBadRequestWithoutChangingRows(String json) throws Exception {
        insertDirectly("Существующий", 42);
        List<List<Object>> before = storedRows();
        mockMvc.perform(post("/guild/recruits")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        assertEquals(before, storedRows());
    }

    private static Stream<String> invalidRequests() {
        return Stream.of(
                "{\"level\":25}",
                "{\"name\":null,\"level\":25}",
                "{\"name\":\"\",\"level\":25}",
                "{\"name\":\"   \",\"level\":25}",
                "{\"name\":\"\\t\\n\",\"level\":25}",
                "{\"name\":\"" + "А".repeat(31) + "\",\"level\":25}",
                "{\"name\":\"Лира\"}",
                "{\"name\":\"Лира\",\"level\":null}",
                "{\"name\":\"Лира\",\"level\":0}",
                "{\"name\":\"Лира\",\"level\":-1}",
                "{\"name\":\"Лира\",\"level\":101}",
                "{\"name\":\"Лира\",\"level\":2147483648}",
                "{\"name\":\"Лира\",\"level\":\"abc\"}",
                "{}", "", "null", "[]", "{\"name\":\"Лира\","
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "999", "9223372036854775807", "-9223372036854775808"})
    void missingLongIdReturnsNotFound(String id) throws Exception {
        long storedId = insertDirectly("Сохранённый", 25);
        List<List<Object>> before = storedRows();
        mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
        assertEquals(before, storedRows());
        expectRecruit(storedId, "Сохранённый", 25);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "-9223372036854775809"})
    void invalidGetIdReturnsBadRequestWithoutChangingRows(String id) throws Exception {
        insertDirectly("Сохранённый", 25);
        List<List<Object>> before = storedRows();
        mockMvc.perform(get("/guild/recruits/{id}", id)).andExpect(status().isBadRequest());
        assertEquals(before, storedRows());
    }

    @Test
    void getDoesNotChangeRows() throws Exception {
        long id = create("Лира", 25);
        List<List<Object>> before = storedRows();
        expectRecruit(id, "Лира", 25);
        expectRecruit(id, "Лира", 25);
        assertEquals(before, storedRows());
    }

    private long create(String name, int level) throws Exception {
        String json = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("name", name).put("level", level));
        MvcResult result = mockMvc.perform(post("/guild/recruits")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);
        JsonNode uri = objectMapper.createObjectNode().put("location", location);
        String path = java.net.URI.create(uri.path("location").asText()).getPath();
        assertTrue(path.matches("/guild/recruits/[1-9][0-9]*"));
        long locationId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(response.path("id").isIntegralNumber());
        assertEquals(locationId, response.path("id").asLong());
        assertTrue(response.path("level").isIntegralNumber());
        assertEquals(level, response.path("level").asInt());
        assertTrue(response.path("name").isTextual());
        assertEquals(name, response.path("name").asText());
        return locationId;
    }

    private void expectRecruit(long id, String name, int level) throws Exception {
        MvcResult result = mockMvc.perform(get("/guild/recruits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(response.path("id").isIntegralNumber());
        assertEquals(id, response.path("id").asLong());
        assertTrue(response.path("name").isTextual());
        assertEquals(name, response.path("name").asText());
        assertTrue(response.path("level").isIntegralNumber());
        assertEquals(level, response.path("level").asInt());
    }

    private long insertDirectly(String name, int level) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO guild_recruits (name, level) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, name);
            insert.setInt(2, level);
            assertEquals(1, insert.executeUpdate());
            try (ResultSet keys = insert.getGeneratedKeys()) {
                assertTrue(keys.next());
                long id = keys.getLong(1);
                assertTrue(id > 0);
                return id;
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
