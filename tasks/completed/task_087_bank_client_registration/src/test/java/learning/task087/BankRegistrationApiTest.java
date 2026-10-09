package learning.task087;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BankRegistrationApiTest {
    private static final String CLIENT_PASSWORD = "Client_87!";
    private static final String AUDITOR_PASSWORD = "Audit_87!";
    private static final BCryptPasswordEncoder FIXTURES = new BCryptPasswordEncoder(4);
    private static PostgresTestDatabase database;
    private final ObjectMapper json = new ObjectMapper();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;
    @Autowired UserDetailsService users;
    @Autowired PasswordEncoder encoder;
    @Autowired ConfigurableApplicationContext context;

    @DynamicPropertySource
    static void isolatedPostgres(DynamicPropertyRegistry registry) {
        if (database == null) database = new PostgresTestDatabase();
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
    }

    @BeforeAll
    void checksDeclaredConfigurationMigrationAndEmptyJpaTable() throws Exception {
        Properties properties = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "Самостоятельно создай application.properties.");
            properties.load(input);
        }
        Map<String, String> expected = Map.of("spring.datasource.url", "${COURSE_PG_URL}",
                "spring.datasource.username", "${COURSE_PG_USER}",
                "spring.datasource.password", "${COURSE_PG_PASSWORD}",
                "spring.jpa.hibernate.ddl-auto", "validate", "spring.jpa.open-in-view", "false");
        expected.forEach((key, value) -> assertEquals(value, properties.getProperty(key), key));
        assertEquals("validate", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", context.getEnvironment().getProperty("spring.jpa.open-in-view"));
        assertNotNull(getClass().getClassLoader().getResource("db/migration/V1__create_bank_users.sql"));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(database.schema(), connection.getSchema());
            try (var rows = statement.executeQuery("SELECT id, username, password_hash, role FROM bank_users WHERE 1 = 0")) {
                int[] types = {Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
                int[] lengths = {0, 40, 100, 16};
                var meta = rows.getMetaData();
                for (int i = 0; i < types.length; i++) {
                    assertEquals(types[i], meta.getColumnType(i + 1));
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
        assertEquals(Long.class, entity.getIdType().getJavaType());
        assertEquals("", entity.getJavaType().getAnnotation(Table.class).schema());
        var history = jdbc.queryForList("SELECT version, script, checksum, success FROM flyway_schema_history");
        assertEquals(1, history.size());
        assertEquals("1", history.getFirst().get("version"));
        assertEquals("V1__create_bank_users.sql", history.getFirst().get("script"));
        assertNotNull(history.getFirst().get("checksum"));
        assertEquals(Boolean.TRUE, history.getFirst().get("success"));
        assertEquals(List.of(), stored(), "При запуске таблица должна быть пустой.");
    }

    @BeforeEach
    void preparesIndependentAccounts() {
        jdbc.update("DELETE FROM bank_users");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('bank_users', 'id'), 100, false)");
        insert(11, "alex", CLIENT_PASSWORD, "CLIENT");
        insert(22, "nina", AUDITOR_PASSWORD, "AUDITOR");
    }

    @AfterAll
    void removesOnlyThisRunsSchema() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void publicRoutesAndAnonymousProtectedRoutesHaveExpectedAccess() throws Exception {
        read(get("/bank/info"), 200, Map.of("name", "Practice Bank"));
        read(basic(get("/bank/info"), "alex", CLIENT_PASSWORD), 200, Map.of("name", "Practice Bank"));
        read(basic(get("/bank/info"), "nina", AUDITOR_PASSWORD), 200, Map.of("name", "Practice Bank"));
        token();
        for (String path : List.of("/bank/me", "/bank/client-count", "/unmapped")) {
            read(get(path), 401, null).andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Basic")));
        }
        tokenWithUser("nina", AUDITOR_PASSWORD);
        tokenWithUser("alex", CLIENT_PASSWORD);
        for (String path : List.of("/bank/me", "/bank/client-count")) {
            read(basic(get(path), "unknown", "Unknown_87!"), 401, null);
            read(basic(get(path), "alex", "wrong"), 401, null);
        }
    }

    @Test
    void registrationStoresBcryptHashAndImmediatelyAllowsBasicLogin() throws Exception {
        long id = register("maria", "NewPass_87!", Map.of(), null, null);
        assertTrue(id > 0);
        profile("maria", "NewPass_87!", "CLIENT");
        read(basic(get("/bank/client-count"), "maria", "NewPass_87!"), 403, null);
        read(basic(get("/bank/client-count"), "nina", AUDITOR_PASSWORD), 200, Map.of("count", 2));
        read(basic(get("/bank/me"), "maria", "incorrect"), 401, null);
    }

    @Test
    void ignoresClientSuppliedRoleIdAndHashForAnonymousAndAuditorRegistration() throws Exception {
        register("guest", "Guest_87!", Map.of("role", "AUDITOR", "id", Long.MAX_VALUE,
                "passwordHash", FIXTURES.encode("Injected_87!")), null, null);
        register("newclient", "Other_87!", Map.of("role", "AUDITOR"), "nina", AUDITOR_PASSWORD);
        profile("guest", "Guest_87!", "CLIENT");
        profile("newclient", "Other_87!", "CLIENT");
        read(basic(get("/bank/client-count"), "guest", "Guest_87!"), 403, null);
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM bank_users WHERE role = 'AUDITOR'", Long.class));
    }

    @Test
    void repeatedPlainPasswordProducesDifferentSaltedHashes() throws Exception {
        register("first", "Shared_87!", Map.of(), null, null);
        register("second", "Shared_87!", Map.of(), null, null);
        assertNotEquals(row("first").get("password_hash"), row("second").get("password_hash"));
        profile("first", "Shared_87!", "CLIENT");
        profile("second", "Shared_87!", "CLIENT");
    }

    @Test
    void acceptsUsernameAndPasswordBoundariesWithoutNormalizing() throws Exception {
        register("x", "A".repeat(8), Map.of(), null, null);
        register("П".repeat(40), "Z".repeat(40), Map.of(), null, null);
        register(" Олег \"Тест\" ", "Symbols_87!", Map.of(), null, null);
        register("Alex", "Case_87!", Map.of(), null, null);
        profile("x", "A".repeat(8), "CLIENT");
        profile("П".repeat(40), "Z".repeat(40), "CLIENT");
        profile(" Олег \"Тест\" ", "Symbols_87!", "CLIENT");
        profile("Alex", "Case_87!", "CLIENT");
        profile("alex", CLIENT_PASSWORD, "CLIENT");
    }

    @Test
    void acceptsBigGeneratedIdIncludingLongMaxValue() throws Exception {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('bank_users', 'id'), 9223372036854775807, false)");
        assertEquals(Long.MAX_VALUE, register("big", "Boundary_87!", Map.of(), null, null));
        profile("big", "Boundary_87!", "CLIENT");
    }

    @Test
    void invalidInputsAndMalformedJsonReturn400WithoutDatabaseChanges() throws Exception {
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (String bad : List.of("", " ", "\t\n", "U".repeat(41), "bad:name")) bodies.add(Map.of("username", bad, "password", "Valid_87!"));
        for (String bad : List.of("", "P".repeat(7), "P".repeat(41), "with space!", "Русский87", "Tabs\t_87!")) bodies.add(Map.of("username", "valid", "password", bad));
        bodies.add(Map.of("password", "Valid_87!"));
        bodies.add(Map.of("username", "valid"));
        bodies.add(Map.of());
        var nullUsername = new HashMap<String, Object>(); nullUsername.put("username", null); nullUsername.put("password", "Valid_87!"); bodies.add(nullUsername);
        var nullPassword = new HashMap<String, Object>(); nullPassword.put("username", "valid"); nullPassword.put("password", null); bodies.add(nullPassword);
        for (var body : bodies) reject(json.writeValueAsString(body), token(), 400, null, null);
        for (String bad : List.of("", "null", "[]", "{broken")) reject(bad, token(), 400, null, null);
    }

    @Test
    void duplicateLoginReturns409AndPreservesExistingPasswordAndRole() throws Exception {
        register("sam", "Original_87!", Map.of(), null, null);
        reject(json.writeValueAsString(Map.of("username", "sam", "password", "Changed_87!", "role", "AUDITOR")), token(), 409, null, null);
        reject(json.writeValueAsString(Map.of("username", "nina", "password", "Changed_87!")), token(), 409, null, null);
        profile("sam", "Original_87!", "CLIENT");
        profile("nina", AUDITOR_PASSWORD, "AUDITOR");
        read(basic(get("/bank/me"), "sam", "Changed_87!"), 401, null);
    }

    @Test
    void missingWrongAndOtherSessionCsrfTokensReturn403BeforeRegistration() throws Exception {
        String body = json.writeValueAsString(Map.of("username", "blocked", "password", "Blocked_87!"));
        Csrf first = token();
        Csrf other = token();
        for (String user : List.of("", "nina")) {
            String password = user.isEmpty() ? null : AUDITOR_PASSWORD;
            String login = user.isEmpty() ? null : user;
            reject(body, null, 403, login, password);
            reject(body, new Csrf(first.session(), first.header(), "incorrect"), 403, login, password);
            reject(body, new Csrf(other.session(), first.header(), first.token()), 403, login, password);
            reject(body, new Csrf(null, first.header(), first.token()), 403, login, password);
        }
    }

    @Test
    void authorizationAndProfileUseCurrentDatabaseState() throws Exception {
        register("reader", "Reader_87!", Map.of(), null, null);
        read(basic(get("/bank/me").param("username", "nina"), "reader", "Reader_87!"), 200, Map.of("username", "reader", "role", "CLIENT"));
        jdbc.update("UPDATE bank_users SET role = 'AUDITOR', password_hash = ? WHERE username = ?", FIXTURES.encode("Updated_87!"), "reader");
        read(basic(get("/bank/me"), "reader", "Reader_87!"), 401, null);
        profile("reader", "Updated_87!", "AUDITOR");
        read(basic(get("/bank/client-count"), "reader", "Updated_87!"), 200, Map.of("count", 1));
        jdbc.update("DELETE FROM bank_users WHERE role = 'CLIENT'");
        read(basic(get("/bank/client-count"), "nina", AUDITOR_PASSWORD), 200, Map.of("count", 0));
        jdbc.update("DELETE FROM bank_users WHERE username = 'reader'");
        read(basic(get("/bank/me"), "reader", "Updated_87!"), 401, null);
    }

    @Test
    void userDetailsExposesStoredHashAndRoleWithoutReencoding() throws Exception {
        register("login", "Login_87!", Map.of(), null, null);
        var details = users.loadUserByUsername("login");
        assertEquals("login", details.getUsername());
        assertEquals(row("login").get("password_hash"), details.getPassword());
        assertTrue(encoder.matches("Login_87!", details.getPassword()));
        assertEquals(List.of("ROLE_CLIENT"), details.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        assertThrows(UsernameNotFoundException.class, () -> users.loadUserByUsername("unknown"));
    }

    @Test
    void postgresEnforcesPrimaryUniqueNotNullLengthAndRoleConstraints() {
        String hash = FIXTURES.encode("Database_87!");
        Object[][] invalid = {{"alex", hash, "CLIENT"}, {null, hash, "CLIENT"}, {"", hash, "CLIENT"},
                {"L".repeat(41), hash, "CLIENT"}, {"valid", null, "CLIENT"}, {"valid", "H".repeat(101), "CLIENT"},
                {"valid", hash, null}, {"valid", hash, "ADMIN"}, {"valid", hash, "client"}, {"valid", hash, ""}};
        var before = stored();
        for (Object[] values : invalid) {
            assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO bank_users(username, password_hash, role) VALUES (?, ?, ?)", values));
            assertEquals(before, stored());
        }
        assertThrows(DataAccessException.class, () -> insert(11, "another", "Database_87!", "CLIENT"));
        assertEquals(before, stored());
    }

    private record Csrf(MockHttpSession session, String header, String token) { }

    private Csrf token() throws Exception { return tokenWithUser(null, null); }

    private Csrf tokenWithUser(String username, String password) throws Exception {
        var before = stored();
        var result = mvc.perform(basic(get("/bank/csrf"), username, password)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json")).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(Set.of("headerName", "token"), keys(body));
        assertEquals("X-CSRF-TOKEN", body.path("headerName").asText());
        assertTrue(body.path("token").isTextual() && !body.path("token").asText().isBlank());
        var session = result.getRequest().getSession(false);
        assertNotNull(session, "Стандартный CSRF-токен должен храниться в HTTP-сессии.");
        assertEquals(before, stored());
        return new Csrf((MockHttpSession) session, body.path("headerName").asText(), body.path("token").asText());
    }

    private long register(String username, String password, Map<String, Object> extras, String login, String loginPassword) throws Exception {
        var before = stored();
        Map<String, Object> input = new HashMap<>(extras); input.put("username", username); input.put("password", password);
        var result = mvc.perform(basic(withCsrf(post("/bank/register"), token()).contentType("application/json")
                .content(json.writeValueAsString(input)), login, loginPassword))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith("application/json")).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(Set.of("id", "username", "role"), keys(body));
        assertTrue(body.path("id").isIntegralNumber() && body.path("id").canConvertToLong());
        assertEquals(username, body.path("username").asText()); assertEquals("CLIENT", body.path("role").asText());
        Map<String, Object> created = row(username);
        assertEquals(body.path("id").longValue(), ((Number) created.get("id")).longValue());
        assertEquals("CLIENT", created.get("role"));
        String hash = (String) created.get("password_hash");
        assertTrue(hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"), "Сохрани BCrypt-хеш.");
        assertNotEquals(password, hash); assertTrue(encoder.matches(password, hash));
        assertTrue(FIXTURES.matches(password, hash));
        assertEquals(before, stored().stream().filter(r -> !r.get("username").equals(username)).toList());
        assertEquals(before.size() + 1, stored().size());
        return body.path("id").longValue();
    }

    private void reject(String body, Csrf csrf, int expected, String username, String password) throws Exception {
        var before = stored();
        mvc.perform(basic(withCsrf(post("/bank/register"), csrf).contentType("application/json").content(body), username, password))
                .andExpect(status().is(expected));
        assertEquals(before, stored(), "Отклонённая регистрация не изменяет строки и хеши.");
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, Csrf csrf) {
        if (csrf == null) return request;
        if (csrf.session() != null) request.session(csrf.session());
        return request.header(csrf.header(), csrf.token());
    }

    private MockHttpServletRequestBuilder basic(MockHttpServletRequestBuilder request, String username, String password) {
        if (username == null) return request;
        return request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    private org.springframework.test.web.servlet.ResultActions read(MockHttpServletRequestBuilder request, int expected, Object body) throws Exception {
        var before = stored();
        var result = mvc.perform(request).andExpect(status().is(expected));
        if (body != null) result.andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json(json.writeValueAsString(body), JsonCompareMode.STRICT));
        assertEquals(before, stored(), "GET не изменяет таблицу.");
        return result;
    }

    private void profile(String username, String password, String role) throws Exception {
        read(basic(get("/bank/me"), username, password), 200, Map.of("username", username, "role", role));
    }

    private Set<String> keys(JsonNode body) {
        Set<String> keys = new HashSet<>(); body.fieldNames().forEachRemaining(keys::add); return keys;
    }

    private void insert(long id, String username, String password, String role) {
        jdbc.update("INSERT INTO bank_users(id, username, password_hash, role) VALUES (?, ?, ?, ?)", id, username, FIXTURES.encode(password), role);
    }

    private List<Map<String, Object>> stored() { return jdbc.queryForList("SELECT id, username, password_hash, role FROM bank_users ORDER BY id"); }
    private Map<String, Object> row(String username) { return jdbc.queryForMap("SELECT id, username, password_hash, role FROM bank_users WHERE username = ?", username); }
}
