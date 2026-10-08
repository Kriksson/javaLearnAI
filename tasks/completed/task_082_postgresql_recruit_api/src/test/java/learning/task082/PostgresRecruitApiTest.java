package learning.task082;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.repository.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresRecruitApiTest {
    private static PostgresTestDatabase database;

    @DynamicPropertySource
    static void isolatedPostgres(DynamicPropertyRegistry registry) {
        if (database == null) database = new PostgresTestDatabase();
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;
    @Autowired ConfigurableApplicationContext context;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void verifiesRealPostgresConfigurationAndIdentitySchema() throws Exception {
        Properties config = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Создай application.properties самостоятельно.");
            config.load(input);
        }
        assertEquals("${COURSE_PG_URL}", config.getProperty("spring.datasource.url"));
        assertEquals("${COURSE_PG_USER}", config.getProperty("spring.datasource.username"));
        assertEquals("${COURSE_PG_PASSWORD}", config.getProperty("spring.datasource.password"));
        assertEquals("update", config.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", config.getProperty("spring.jpa.open-in-view"));
        assertEquals("update", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertFalse(context.getBeansOfType(Repository.class).isEmpty(), "Используй Spring Data JPA.");
        var entity = emf.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().getPackageName().startsWith("learning.task082"))
                .filter(e -> e.getJavaType().isAnnotationPresent(Table.class)
                        && e.getJavaType().getAnnotation(Table.class).name().equals("recruits"))
                .findFirst().orElseThrow(() -> new AssertionError("Создай JPA-сущность для recruits."));
        assertEquals("", entity.getJavaType().getAnnotation(Table.class).schema(),
                "Не фиксируй schema в @Table: тесты используют отдельную схему.");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(database.schema(), connection.getSchema());
            try (var rows = statement.executeQuery("SELECT id, name, level FROM recruits WHERE 1 = 0")) {
                var columns = rows.getMetaData();
                int[] types = {Types.BIGINT, Types.VARCHAR, Types.INTEGER};
                for (int i = 0; i < types.length; i++) {
                    assertEquals(types[i], columns.getColumnType(i + 1));
                    assertEquals(java.sql.ResultSetMetaData.columnNoNulls, columns.isNullable(i + 1));
                }
                assertEquals(40, columns.getPrecision(2));
                assertTrue(columns.isAutoIncrement(1), "ID должен выдаваться базой.");
            }
            try (var keys = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), database.schema(), "recruits")) {
                assertTrue(keys.next());
                assertEquals("id", keys.getString("COLUMN_NAME"));
                assertFalse(keys.next());
            }
        }
        assertEquals("YES", jdbc.queryForObject("SELECT is_identity FROM information_schema.columns WHERE table_schema = ? AND table_name = 'recruits' AND column_name = 'id'", String.class, database.schema()));
        assertEquals(List.of(), stored());
    }

    @BeforeEach
    void clearsOnlyThisRunsRows() { jdbc.update("DELETE FROM recruits"); }

    @AfterAll
    void removesOnlyThisRunsSchema() throws Exception { if (database != null) database.close(); }

    @Test
    void createsRecruitWithDatabaseIdLocationAndReadableDto() throws Exception {
        long id = create("Mira", 3);
        assertEquals(List.of(List.of(id, "Mira", 3)), stored());
        assertRead(id, 200, dto(id, "Mira", 3));
    }

    @Test
    void acceptsBoundaryLevelsAndPreservesNames() throws Exception {
        String name = " Рин \"Лис\" ";
        long first = create(name, 0);
        long second = create("Г".repeat(40), Integer.MAX_VALUE);
        assertNotEquals(first, second);
        assertRead(first, 200, dto(first, name, 0));
        assertRead(second, 200, dto(second, "Г".repeat(40), Integer.MAX_VALUE));
    }

    @Test
    void repeatedPostCreatesTwoRowsWithoutOverwritingTheFirst() throws Exception {
        long first = create("Mira", 3);
        long second = create("Mira", 3);
        assertNotEquals(first, second);
        assertEquals(2, stored().size());
        assertRead(first, 200, dto(first, "Mira", 3));
        assertRead(second, 200, dto(second, "Mira", 3));
    }

    @Test
    void rejectsInvalidBodiesWithoutChangingRows() throws Exception {
        create("Existing", 2);
        List<List<Object>> before = stored();
        for (String body : new String[]{"", "null", "{}", "{", "{\"name\":\"Mira\"}",
                "{\"level\":2}", "{\"name\":null,\"level\":2}", "{\"name\":\"\",\"level\":2}",
                "{\"name\":\"   \",\"level\":2}", dtoBody("N".repeat(41), 2),
                "{\"name\":\"Mira\",\"level\":null}", dtoBody("Mira", -1),
                "{\"name\":\"Mira\",\"level\":2147483648}", "{\"name\":\"Mira\",\"level\":\"abc\"}"}) {
            mvc.perform(post("/recruits").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            assertEquals(before, stored());
        }
    }

    @Test
    void readsCurrentDatabaseValuesBigIdsAndMissingIds() throws Exception {
        long id = 3_000_000_000L;
        jdbc.update("INSERT INTO recruits (id, name, level) VALUES (?, ?, ?)", id, "Before", 3);
        assertRead(id, 200, dto(id, "Before", 3));
        jdbc.update("UPDATE recruits SET name = ?, level = ? WHERE id = ?", "After", 0, id);
        assertRead(id, 200, dto(id, "After", 0));
        assertRead(9_000_000_000L, 404, null);
        jdbc.update("DELETE FROM recruits WHERE id = ?", id);
        assertRead(id, 404, null);
    }

    @Test
    void databaseRejectsNullNameAndLevel() {
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO recruits (name, level) VALUES (?, ?)", null, 3));
        assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO recruits (name, level) VALUES (?, ?)", "Mira", null));
        assertEquals(List.of(), stored());
    }

    @Test
    void dataSurvivesClosingAndReopeningApplicationContexts() throws Exception {
        long id = create("Persistent", 7);
        var bootNames = context.getBeanNamesForAnnotation(SpringBootApplication.class);
        assertEquals(1, bootNames.length, "Нужен один стартовый класс Spring Boot.");
        Class<?> bootClass = ClassUtils.getUserClass(context.getType(bootNames[0]));
        try (var reopened = reopen(bootClass)) { assertJpaReadsSavedRow(reopened, id); }
        assertEquals(List.of(List.of(id, "Persistent", 7)), stored(),
                "Закрытие контекста не должно удалять таблицу или данные.");
        try (var reopenedAgain = reopen(bootClass)) { assertJpaReadsSavedRow(reopenedAgain, id); }
        assertRead(id, 200, dto(id, "Persistent", 7));
    }

    private ConfigurableApplicationContext reopen(Class<?> bootClass) {
        var app = new SpringApplication(bootClass);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setLogStartupInfo(false);
        app.addInitializers(reopened -> reopened.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("task082-isolated-connection", Map.of(
                        "spring.datasource.url", database.url(),
                        "spring.datasource.username", database.username(),
                        "spring.datasource.password", database.password()))));
        return app.run();
    }

    private void assertJpaReadsSavedRow(ConfigurableApplicationContext reopened, long id) throws Exception {
        var factory = reopened.getBean(EntityManagerFactory.class);
        var type = factory.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().isAnnotationPresent(Table.class)
                        && e.getJavaType().getAnnotation(Table.class).name().equals("recruits"))
                .findFirst().orElseThrow();
        try (var manager = factory.createEntityManager()) {
            Object recruit = manager.find(type.getJavaType(), id);
            assertNotNull(recruit, "Новый JPA-контекст должен читать сохранённую запись PostgreSQL.");
            assertEquals(id, factory.getPersistenceUnitUtil().getIdentifier(recruit));
        }
        assertEquals(List.of(List.of(id, "Persistent", 7)), reopened.getBean(JdbcTemplate.class)
                .query("SELECT id, name, level FROM recruits ORDER BY id", (r, n) -> List.of(r.getLong("id"), r.getString("name"), r.getInt("level"))));
    }

    private long create(String name, int level) throws Exception {
        List<List<Object>> before = stored();
        var response = mvc.perform(post("/recruits").contentType(MediaType.APPLICATION_JSON)
                        .content(dtoBody(name, level)))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        JsonNode body = json.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(body.has("id") && body.get("id").isIntegralNumber() && body.get("id").canConvertToLong());
        long id = body.get("id").longValue();
        assertTrue(id > 0);
        assertEquals("/recruits/" + id, response.getHeader("Location"));
        assertEquals(json.readTree(dto(id, name, level)), body);
        List<List<Object>> after = stored();
        assertEquals(before.size() + 1, after.size());
        assertTrue(after.containsAll(before), "POST не должен менять прежние строки.");
        assertTrue(after.contains(List.of(id, name, level)), "ID ответа должен быть настоящим ID базы.");
        return id;
    }

    private void assertRead(long id, int expectedStatus, String expectedJson) throws Exception {
        var before = stored();
        var result = mvc.perform(get("/recruits/{id}", id)).andExpect(status().is(expectedStatus));
        if (expectedJson != null) result.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedJson, JsonCompareMode.STRICT));
        assertEquals(before, stored(), "GET не должен изменять строки.");
    }

    private String dto(long id, String name, int level) throws Exception {
        return json.writeValueAsString(Map.of("id", id, "name", name, "level", level));
    }

    private String dtoBody(String name, int level) throws Exception {
        return json.writeValueAsString(Map.of("name", name, "level", level));
    }

    private List<List<Object>> stored() {
        return jdbc.query("SELECT id, name, level FROM recruits ORDER BY id",
                (r, n) -> List.of(r.getLong("id"), r.getString("name"), r.getInt("level")));
    }
}
