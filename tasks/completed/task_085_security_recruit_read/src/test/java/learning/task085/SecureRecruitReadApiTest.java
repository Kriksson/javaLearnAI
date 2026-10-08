package learning.task085;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecureRecruitReadApiTest {
    private static final String READER_PASSWORD = "ReaderTest_85!";
    private static final String ADMIN_PASSWORD = "AdminTest_85!";
    private static PostgresTestDatabase database;
    private static final long BIG_ID = 3_000_000_000L;
    private final ObjectMapper json = new ObjectMapper();

    @DynamicPropertySource
    static void isolatedPostgresAndTestPasswords(DynamicPropertyRegistry registry) {
        if (database == null) database = new PostgresTestDatabase();
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("guild.security.reader-password", () -> READER_PASSWORD);
        registry.add("guild.security.admin-password", () -> ADMIN_PASSWORD);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;
    @Autowired ConfigurableApplicationContext context;

    @BeforeAll
    void verifiesExternalConfigurationMigrationsAndJpaSchema() throws Exception {
        Properties config = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Создай application.properties самостоятельно.");
            config.load(input);
        }
        assertEquals("${COURSE_PG_URL}", config.getProperty("spring.datasource.url"));
        assertEquals("${COURSE_PG_USER}", config.getProperty("spring.datasource.username"));
        assertEquals("${COURSE_PG_PASSWORD}", config.getProperty("spring.datasource.password"));
        assertEquals("${COURSE_READER_PASSWORD}", config.getProperty("guild.security.reader-password"));
        assertEquals("${COURSE_ADMIN_PASSWORD}", config.getProperty("guild.security.admin-password"));
        assertEquals("validate", config.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", config.getProperty("spring.jpa.open-in-view"));
        assertEquals("validate", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(database.schema(), connection.getSchema());
            try (var rows = statement.executeQuery("SELECT id, name, level, coins FROM recruits WHERE 1 = 0")) {
                int[] expected = {Types.BIGINT, Types.VARCHAR, Types.INTEGER, Types.INTEGER};
                for (int c = 0; c < expected.length; c++) {
                    assertEquals(expected[c], rows.getMetaData().getColumnType(c + 1));
                    assertEquals(java.sql.ResultSetMetaData.columnNoNulls, rows.getMetaData().isNullable(c + 1));
                }
                assertEquals(40, rows.getMetaData().getPrecision(2));
                assertTrue(rows.getMetaData().isAutoIncrement(1));
            }
        }
        var entity = emf.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().isAnnotationPresent(Table.class)
                        && "recruits".equals(e.getJavaType().getAnnotation(Table.class).name())).findFirst().orElseThrow();
        assertEquals("", entity.getJavaType().getAnnotation(Table.class).schema());
        var history = jdbc.queryForList("SELECT version, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank");
        assertEquals(2, history.size());
        assertEquals(List.of("1", "2"), history.stream().map(row -> row.get("version")).toList());
        assertEquals(List.of("V1__create_recruits.sql", "V2__add_recruit_coins.sql"), history.stream().map(row -> row.get("script")).toList());
        assertEquals(List.of(-710521246, 1637954827), history.stream().map(row -> row.get("checksum")).toList(),
                "Перенеси обе миграции из архива №84 без изменений.");
        assertTrue(history.stream().allMatch(row -> Boolean.TRUE.equals(row.get("success"))));
        assertEquals(List.of(), stored(), "Не добавляй начальных рекрутов при запуске.");
    }

    @BeforeEach
    void preparesOnlyThisRunsRows() {
        jdbc.update("DELETE FROM recruits");
        jdbc.update("INSERT INTO recruits (id, name, level, coins) VALUES (?, ?, ?, ?)", 11L, "Mira", 3, 5);
        jdbc.update("INSERT INTO recruits (id, name, level, coins) VALUES (?, ?, ?, ?)", BIG_ID, " Рин \"Лис\" ", Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @AfterAll
    void removesOnlyThisRunsSchema() throws Exception { if (database != null) database.close(); }

    @Test
    void publicGuildInfoIsReadableWithoutCredentialsAndWithBothUsers() throws Exception {
        for (String user : new String[]{null, "reader", "admin"}) {
            var before = stored();
            mvc.perform(auth(get("/guild/info"), user)).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(content().json("{\"name\":\"Training Guild\"}", JsonCompareMode.STRICT));
            assertEquals(before, stored());
        }
    }

    @Test
    void anonymousProtectedRequestsReturn401BeforeResourceLookup() throws Exception {
        for (String path : new String[]{"/recruits/11", "/recruits/9000000000", "/guild/recruit-count", "/unmapped"}) {
            rejected(get(path), 401);
        }
    }

    @Test
    void unknownUsersAndWrongOrMissingPasswordsReturn401() throws Exception {
        for (String[] credentials : new String[][]{{"reader", "wrong"}, {"admin", "wrong"},
                {"unknown", READER_PASSWORD}, {"reader", ""}, {"reader", ADMIN_PASSWORD}, {"admin", READER_PASSWORD}}) {
            rejected(get("/recruits/11").header("Authorization", basic(credentials[0], credentials[1])), 401);
        }
    }

    @Test
    void readerReadsCurrentValuesBigIdsAnd404AfterDeletion() throws Exception {
        read("reader", 11, 200, dto(11, "Mira", 3, 5));
        read("reader", BIG_ID, 200, dto(BIG_ID, " Рин \"Лис\" ", Integer.MAX_VALUE, Integer.MAX_VALUE));
        jdbc.update("UPDATE recruits SET name = ?, level = ?, coins = ? WHERE id = ?", "Г".repeat(40), 0, 0, BIG_ID);
        read("reader", BIG_ID, 200, dto(BIG_ID, "Г".repeat(40), 0, 0));
        read("reader", 9_000_000_000L, 404, null);
        jdbc.update("DELETE FROM recruits WHERE id = ?", BIG_ID);
        read("reader", BIG_ID, 404, null);
    }

    @Test
    void readerCannotReadAdministratorCountEvenWhenDatabaseIsEmpty() throws Exception {
        rejected(auth(get("/guild/recruit-count"), "reader"), 403);
        jdbc.update("DELETE FROM recruits");
        rejected(auth(get("/guild/recruit-count"), "reader"), 403);
    }

    @Test
    void administratorAlsoReadsRecruitsAndGets404ForMissingRecruit() throws Exception {
        read("admin", 11, 200, dto(11, "Mira", 3, 5));
        read("admin", BIG_ID, 200, dto(BIG_ID, " Рин \"Лис\" ", Integer.MAX_VALUE, Integer.MAX_VALUE));
        read("admin", 9_000_000_000L, 404, null);
    }

    @Test
    void administratorCountReflectsInsertsDeletesAndEmptyDatabase() throws Exception {
        count(2);
        jdbc.update("INSERT INTO recruits (id, name, level) VALUES (?, ?, ?)", 12L, "New", 0);
        count(3);
        jdbc.update("DELETE FROM recruits WHERE id = ?", 11L);
        count(2);
        jdbc.update("DELETE FROM recruits");
        count(0);
    }

    @Test
    void userDetailsHaveRequiredRolesAndBcryptPasswordsFromConfiguration() throws Exception {
        // Standard Spring Security interfaces, loaded reflectively so initial tests compile
        // before the learner adds the application security dependency.
        Class<?> serviceType = Class.forName("org.springframework.security.core.userdetails.UserDetailsService");
        Class<?> userType = Class.forName("org.springframework.security.core.userdetails.UserDetails");
        Class<?> authorityType = Class.forName("org.springframework.security.core.GrantedAuthority");
        Class<?> encoderType = Class.forName("org.springframework.security.crypto.password.PasswordEncoder");
        Object service = context.getBean(serviceType);
        Object encoder = context.getBean(encoderType);
        for (String username : new String[]{"reader", "admin"}) {
            Object user = serviceType.getMethod("loadUserByUsername", String.class).invoke(service, username);
            String hash = (String) userType.getMethod("getPassword").invoke(user);
            String raw = username.equals("reader") ? READER_PASSWORD : ADMIN_PASSWORD;
            assertNotEquals(raw, hash, "Храни в UserDetails BCrypt-хеш.");
            String bcrypt = hash.startsWith("{bcrypt}") ? hash.substring(8) : hash;
            assertTrue(bcrypt.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"), "Используй BCrypt через PasswordEncoder.");
            assertEquals(Boolean.TRUE, encoderType.getMethod("matches", CharSequence.class, String.class).invoke(encoder, raw, hash));
            assertEquals(Boolean.FALSE, encoderType.getMethod("matches", CharSequence.class, String.class).invoke(encoder, "wrong", hash));
            var roles = new HashSet<String>();
            for (Object authority : (Collection<?>) userType.getMethod("getAuthorities").invoke(user)) {
                roles.add((String) authorityType.getMethod("getAuthority").invoke(authority));
            }
            assertEquals(Set.of(username.equals("reader") ? "ROLE_READER" : "ROLE_ADMIN"), roles);
        }
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String user) {
        return user == null ? request : request.header("Authorization", basic(user, user.equals("reader") ? READER_PASSWORD : ADMIN_PASSWORD));
    }

    private String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private void rejected(MockHttpServletRequestBuilder request, int status) throws Exception {
        var before = stored();
        var response = mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse();
        if (status == 401) {
            assertNotNull(response.getHeader("WWW-Authenticate"));
            assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic"));
        }
        assertEquals(before, stored(), "Отклонённый запрос не меняет рекрутов.");
    }

    private void read(String user, long id, int status, String body) throws Exception {
        var before = stored();
        var result = mvc.perform(auth(get("/recruits/{id}", id), user)).andExpect(status().is(status));
        if (body != null) result.andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json(body, JsonCompareMode.STRICT));
        assertEquals(before, stored(), "GET не меняет рекрутов.");
    }

    private void count(long expected) throws Exception {
        var before = stored();
        mvc.perform(auth(get("/guild/recruit-count"), "admin")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json(json.writeValueAsString(Map.of("count", expected)), JsonCompareMode.STRICT));
        assertEquals(before, stored());
    }

    private String dto(long id, String name, int level, int coins) throws Exception {
        return json.writeValueAsString(Map.of("id", id, "name", name, "level", level, "coins", coins));
    }

    private List<List<Object>> stored() {
        return jdbc.query("SELECT id, name, level, coins FROM recruits ORDER BY id",
                (r, n) -> List.of(r.getLong("id"), r.getString("name"), r.getInt("level"), r.getInt("coins")));
    }
}
