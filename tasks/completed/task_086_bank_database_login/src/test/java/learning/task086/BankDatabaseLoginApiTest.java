package learning.task086;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class BankDatabaseLoginApiTest {
    private static final String ALEX_PASSWORD = "AlexTest_86!";
    private static final String NINA_PASSWORD = "NinaTest_86!";
    private static final String SAM_PASSWORD = "SamTest_86!";
    private static final BCryptPasswordEncoder FIXTURE_ENCODER = new BCryptPasswordEncoder(4);
    private static PostgresTestDatabase database;
    private final ObjectMapper json = new ObjectMapper();

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
    @Autowired UserDetailsService users;
    @Autowired PasswordEncoder encoder;
    @Autowired ConfigurableApplicationContext context;

    @BeforeAll
    void checksConfigurationMigrationAndEmptyJpaTable() throws Exception {
        Properties config = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Создай application.properties самостоятельно.");
            config.load(input);
        }
        assertEquals("${COURSE_PG_URL}", config.getProperty("spring.datasource.url"));
        assertEquals("${COURSE_PG_USER}", config.getProperty("spring.datasource.username"));
        assertEquals("${COURSE_PG_PASSWORD}", config.getProperty("spring.datasource.password"));
        assertEquals("validate", config.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", config.getProperty("spring.jpa.open-in-view"));
        assertEquals("validate", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", context.getEnvironment().getProperty("spring.jpa.open-in-view"));
        assertNotNull(getClass().getClassLoader().getResource("db/migration/V1__create_bank_users.sql"),
                "Размести миграцию в src/main/resources/db/migration/.");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(database.schema(), connection.getSchema());
            try (var rows = statement.executeQuery("SELECT id, username, password_hash, role FROM bank_users WHERE 1 = 0")) {
                int[] expectedTypes = {Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
                int[] lengths = {0, 40, 100, 16};
                var meta = rows.getMetaData();
                for (int i = 0; i < expectedTypes.length; i++) {
                    assertEquals(expectedTypes[i], meta.getColumnType(i + 1));
                    assertEquals(java.sql.ResultSetMetaData.columnNoNulls, meta.isNullable(i + 1));
                    if (i > 0) assertEquals(lengths[i], meta.getPrecision(i + 1));
                }
                assertTrue(meta.isAutoIncrement(1));
            }
        }
        var entity = emf.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().isAnnotationPresent(Table.class)
                        && "bank_users".equals(e.getJavaType().getAnnotation(Table.class).name()))
                .findFirst().orElseThrow(() -> new AssertionError("Отобрази JPA-сущность на bank_users."));
        assertEquals("", entity.getJavaType().getAnnotation(Table.class).schema());
        assertEquals(Long.class, entity.getIdType().getJavaType());
        var history = jdbc.queryForList("SELECT version, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank");
        assertEquals(1, history.size());
        assertEquals("1", history.getFirst().get("version"));
        assertEquals("V1__create_bank_users.sql", history.getFirst().get("script"));
        assertNotNull(history.getFirst().get("checksum"));
        assertEquals(Boolean.TRUE, history.getFirst().get("success"));
        assertEquals(List.of(), stored(), "Не создавай начальных пользователей при запуске.");
    }

    @BeforeEach
    void preparesIndependentCredentials() {
        jdbc.update("DELETE FROM bank_users");
        insert(11L, "alex", ALEX_PASSWORD, "CLIENT");
        insert(22L, "nina", NINA_PASSWORD, "AUDITOR");
        insert(3_000_000_000L, "sam", SAM_PASSWORD, "CLIENT");
    }

    @AfterAll
    void removesOnlyThisRunsSchema() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void publicInfoWorksWithoutCredentialsAndWithBothRoles() throws Exception {
        request(get("/bank/info"), null, null, 200, Map.of("name", "Practice Bank"));
        request(get("/bank/info"), "alex", ALEX_PASSWORD, 200, Map.of("name", "Practice Bank"));
        request(get("/bank/info"), "nina", NINA_PASSWORD, 200, Map.of("name", "Practice Bank"));
    }

    @Test
    void protectedRoutesRejectAnonymousUnknownAndIncorrectCredentials() throws Exception {
        for (String path : List.of("/bank/me", "/bank/client-count", "/unmapped")) {
            request(get(path), null, null, 401, null);
        }
        for (String[] credentials : new String[][]{{"unknown", ALEX_PASSWORD}, {"alex", "wrong"},
                {"nina", "wrong"}, {"alex", ""}, {"alex", NINA_PASSWORD}, {"nina", ALEX_PASSWORD},
                {"Alex", ALEX_PASSWORD}}) {
            for (String path : List.of("/bank/me", "/bank/client-count")) {
                request(get(path), credentials[0], credentials[1], 401, null);
            }
        }
    }

    @Test
    void currentProfileIsPrivateAndPreservesUsernameBoundaries() throws Exception {
        profile("alex", ALEX_PASSWORD, "CLIENT");
        profile("nina", NINA_PASSWORD, "AUDITOR");
        profile("sam", SAM_PASSWORD, "CLIENT");
        request(get("/bank/me").param("username", "nina"), "alex", ALEX_PASSWORD, 200,
                Map.of("username", "alex", "role", "CLIENT"));
        insert(40L, "x", "Single_86!", "CLIENT");
        profile("x", "Single_86!", "CLIENT");
        String unicodeName = "П".repeat(40);
        insert(Long.MAX_VALUE, unicodeName, "Unicode_86!", "AUDITOR");
        profile(unicodeName, "Unicode_86!", "AUDITOR");
        insert(41L, " Олег \"Тест\" ", "Space_86!", "CLIENT");
        profile(" Олег \"Тест\" ", "Space_86!", "CLIENT");
    }

    @Test
    void onlyAuditorCountsClientsAndSeesCurrentChangesAndZero() throws Exception {
        request(get("/bank/client-count"), "alex", ALEX_PASSWORD, 403, null);
        count(2);
        insert(33L, "dana", "Dana_86!", "AUDITOR");
        count(2);
        insert(44L, "ivan", "Ivan_86!", "CLIENT");
        count(3);
        jdbc.update("DELETE FROM bank_users WHERE username = ?", "ivan");
        count(2);
        jdbc.update("DELETE FROM bank_users WHERE role = ?", "CLIENT");
        count(0);
        insert(55L, "alex", ALEX_PASSWORD, "CLIENT");
        request(get("/bank/client-count"), "alex", ALEX_PASSWORD, 403, null);
    }

    @Test
    void changedRolesAffectTheNextBasicRequestWithoutRestart() throws Exception {
        request(get("/bank/client-count"), "alex", ALEX_PASSWORD, 403, null);
        jdbc.update("UPDATE bank_users SET role = ? WHERE username = ?", "AUDITOR", "alex");
        profile("alex", ALEX_PASSWORD, "AUDITOR");
        request(get("/bank/client-count"), "alex", ALEX_PASSWORD, 200, Map.of("count", 1));
        jdbc.update("UPDATE bank_users SET role = ? WHERE username = ?", "CLIENT", "nina");
        profile("nina", NINA_PASSWORD, "CLIENT");
        request(get("/bank/client-count"), "nina", NINA_PASSWORD, 403, null);
        request(get("/bank/client-count"), "alex", ALEX_PASSWORD, 200, Map.of("count", 2));
    }

    @Test
    void changedPasswordRejectsTheOldPasswordAndAcceptsTheNewOne() throws Exception {
        profile("alex", ALEX_PASSWORD, "CLIENT");
        String newPassword = "Changed_86!";
        jdbc.update("UPDATE bank_users SET password_hash = ? WHERE username = ?", FIXTURE_ENCODER.encode(newPassword), "alex");
        request(get("/bank/me"), "alex", ALEX_PASSWORD, 401, null);
        profile("alex", newPassword, "CLIENT");
        profile("nina", NINA_PASSWORD, "AUDITOR");
    }

    @Test
    void newlyInsertedUsersCanLoginAndDeletedUsersCannot() throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "");
        String password = "NewUser_86!";
        request(get("/bank/me"), username, password, 401, null);
        insert(60L, username, password, "CLIENT");
        profile(username, password, "CLIENT");
        count(3);
        jdbc.update("DELETE FROM bank_users WHERE username = ?", username);
        request(get("/bank/me"), username, password, 401, null);
        count(2);
        jdbc.update("DELETE FROM bank_users WHERE username = ?", "nina");
        request(get("/bank/client-count"), "nina", NINA_PASSWORD, 401, null);
    }

    @Test
    void userDetailsComeFromCurrentRowsWithUnchangedHashesAndCorrectRoles() {
        for (var entry : List.of(List.of("alex", ALEX_PASSWORD, "CLIENT"), List.of("nina", NINA_PASSWORD, "AUDITOR"))) {
            var details = users.loadUserByUsername(entry.get(0));
            assertEquals(entry.get(0), details.getUsername());
            String hash = jdbc.queryForObject("SELECT password_hash FROM bank_users WHERE username = ?", String.class, entry.get(0));
            assertEquals(hash, details.getPassword(), "Передавай сохранённый хеш без повторного encode.");
            assertTrue(encoder.matches(entry.get(1), details.getPassword()));
            assertFalse(encoder.matches("wrong", details.getPassword()));
            assertEquals(List.of("ROLE_" + entry.get(2)), details.getAuthorities().stream().map(a -> a.getAuthority()).sorted().toList());
        }
        assertThrows(UsernameNotFoundException.class, () -> users.loadUserByUsername("unknown"));
        jdbc.update("UPDATE bank_users SET role = ? WHERE username = ?", "AUDITOR", "sam");
        assertEquals(List.of("ROLE_AUDITOR"), users.loadUserByUsername("sam").getAuthorities().stream().map(a -> a.getAuthority()).toList());
        jdbc.update("DELETE FROM bank_users WHERE username = ?", "sam");
        assertThrows(UsernameNotFoundException.class, () -> users.loadUserByUsername("sam"));
    }

    @Test
    void databaseEnforcesUniquenessLengthsNullabilityAndRoleValues() {
        jdbc.update("DELETE FROM bank_users");
        String hash = FIXTURE_ENCODER.encode("Constraint_86!");
        long id = jdbc.queryForObject("INSERT INTO bank_users (username, password_hash, role) VALUES (?, ?, ?) RETURNING id", Long.class,
                "unique", hash, "CLIENT");
        assertTrue(id > 0);
        var before = stored();
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "INSERT INTO bank_users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                id, "another", hash, "CLIENT"));
        assertEquals(before, stored());
        Object[][] invalid = {
                {"unique", hash, "AUDITOR"}, {null, hash, "CLIENT"}, {"", hash, "CLIENT"},
                {"L".repeat(41), hash, "CLIENT"}, {"valid", null, "CLIENT"},
                {"valid", "H".repeat(101), "CLIENT"}, {"valid", hash, null},
                {"valid", hash, "UNKNOWN"}, {"valid", hash, "R".repeat(17)}
        };
        for (Object[] row : invalid) {
            assertThrows(DataAccessException.class, () -> jdbc.update(
                    "INSERT INTO bank_users (username, password_hash, role) VALUES (?, ?, ?)", row));
            assertEquals(before, stored());
        }
    }

    private void insert(long id, String username, String password, String role) {
        jdbc.update("INSERT INTO bank_users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                id, username, FIXTURE_ENCODER.encode(password), role);
    }

    private void profile(String username, String password, String role) throws Exception {
        request(get("/bank/me"), username, password, 200, Map.of("username", username, "role", role));
    }

    private void count(long expected) throws Exception {
        request(get("/bank/client-count"), "nina", NINA_PASSWORD, 200, Map.of("count", expected));
    }

    private void request(MockHttpServletRequestBuilder request, String username, String password,
                         int status, Map<String, ?> body) throws Exception {
        var before = stored();
        if (username != null) {
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + encoded);
        }
        var result = mvc.perform(request).andExpect(status().is(status));
        if (body != null) {
            result.andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(content().json(json.writeValueAsString(body), JsonCompareMode.STRICT));
        }
        if (status == 401) {
            String challenge = result.andReturn().getResponse().getHeader("WWW-Authenticate");
            assertNotNull(challenge);
            assertTrue(challenge.startsWith("Basic"));
        }
        assertEquals(before, stored(), "GET и отказ в доступе не изменяют учётные записи.");
    }

    private List<List<Object>> stored() {
        return jdbc.query("SELECT id, username, password_hash, role FROM bank_users ORDER BY id",
                (r, n) -> List.of(r.getLong("id"), r.getString("username"), r.getString("password_hash"), r.getString("role")));
    }
}
