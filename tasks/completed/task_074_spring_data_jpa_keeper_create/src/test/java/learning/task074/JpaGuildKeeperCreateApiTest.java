package learning.task074;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.Repository;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:task074-tests;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaGuildKeeperCreateApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    void startsWithConfiguredEmptyJpaTable() throws Exception {
        assertNotNull(dataSource, "Настрой H2.");
        assertNotNull(jdbc, "Настрой источник данных для тестового окружения.");
        assertNotNull(entityManagerFactory, "Используй Spring Data JPA и настрой Hibernate.");
        Properties settings = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Создай src/main/resources/application.properties.");
            settings.load(input);
        }
        assertTrue(settings.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:mem:"),
                "Настрой отдельную in-memory базу H2 в application.properties.");
        assertEquals("create-drop", settings.getProperty("spring.jpa.hibernate.ddl-auto"),
                "Настрой Hibernate создавать схему при запуске и удалять при остановке.");
        assertEquals("create-drop", applicationContext.getEnvironment()
                .getProperty("spring.jpa.hibernate.ddl-auto"));
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                        .map(entity -> entity.getJavaType())
                        .anyMatch(type -> type.getPackageName().startsWith("learning.task074")),
                "Создай JPA-сущность для хранения хранителей.");
        assertFalse(applicationContext.getBeansOfType(Repository.class).isEmpty(),
                "Создай Spring Data JPA репозиторий.");
        assertFalse(applicationContext.getBeansWithAnnotation(RestController.class).isEmpty(),
                "Создай REST-контроллер.");
        assertEquals(List.of(), storedRows(), "Таблица должна начинать работу пустой.");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT id, name, level FROM guild_keepers WHERE 1 = 0")) {
            ResultSetMetaData columns = rows.getMetaData();
            assertEquals(Types.BIGINT, columns.getColumnType(1));
            assertEquals("id", columns.getColumnName(1).toLowerCase(Locale.ROOT));
            assertTrue(columns.isAutoIncrement(1));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(1));
            assertEquals(Types.VARCHAR, columns.getColumnType(2));
            assertEquals("name", columns.getColumnName(2).toLowerCase(Locale.ROOT));
            assertEquals(40, columns.getPrecision(2));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(2));
            assertEquals(Types.INTEGER, columns.getColumnType(3));
            assertEquals("level", columns.getColumnName(3).toLowerCase(Locale.ROOT));
            assertEquals(ResultSetMetaData.columnNoNulls, columns.isNullable(3));
            List<String> primaryKeys = new ArrayList<>();
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(
                    connection.getCatalog(), null, "GUILD_KEEPERS")) {
                while (keys.next()) {
                    primaryKeys.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            assertEquals(List.of("id"), primaryKeys);
        }
    }

    @BeforeEach
    void clearsRows() {
        jdbc.update("DELETE FROM guild_keepers");
    }

    @Test
    void createsKeeperWithDatabaseIdAndLocation() throws Exception {
        String request = "{\"name\":\"Mira\",\"level\":4}";

        MvcResult result = mockMvc.perform(post("/guild/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Mira"))
                .andExpect(jsonPath("$.level").value(4))
                .andExpect(header().exists("Location"))
                .andReturn();

        long id = responseId(result);
        assertTrue(id > 0, "ID должен быть выдан базой при сохранении.");
        assertLocationPointsToKeeper(result, id);
        assertEquals(List.of(List.of(id, "Mira", 4)), storedRows());
    }

    @Test
    void acceptsFortyCharacterNameAndLargestIntLevel() throws Exception {
        String name = "K".repeat(40);
        String request = objectMapper.writeValueAsString(new KeeperRequest(name, Integer.MAX_VALUE));

        MvcResult result = mockMvc.perform(post("/guild/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.level").value(Integer.MAX_VALUE))
                .andReturn();

        long id = responseId(result);
        assertTrue(id > 0);
        assertLocationPointsToKeeper(result, id);
        assertEquals(List.of(List.of(id, name, Integer.MAX_VALUE)), storedRows());
    }

    @Test
    void rejectsMalformedJsonWithoutWritingARow() throws Exception {
        mockMvc.perform(post("/guild/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest());

        assertEquals(List.of(), storedRows());
    }

    private long responseId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.hasNonNull("id"), "Ответ должен содержать сгенерированный id.");
        return body.get("id").longValue();
    }

    private void assertLocationPointsToKeeper(MvcResult result, long id) {
        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);
        assertEquals("/guild/keepers/" + id, URI.create(location).getPath());
    }

    private List<List<Object>> storedRows() {
        return jdbc.query("SELECT id, name, level FROM guild_keepers ORDER BY id",
                (rows, rowNumber) -> List.of(rows.getLong("id"),
                        rows.getString("name"), rows.getInt("level")));
    }

    private record KeeperRequest(String name, int level) {
    }
}
