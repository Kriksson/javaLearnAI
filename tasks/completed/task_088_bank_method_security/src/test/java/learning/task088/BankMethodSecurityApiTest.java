package learning.task088;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BankMethodSecurityApiTest {
    private static final String CLIENT_PASSWORD = "Client_88!";
    private static final String AUDITOR_PASSWORD = "Audit_88!";
    private static final BCryptPasswordEncoder HASHES = new BCryptPasswordEncoder(4);
    private static PostgresTestDatabase database;
    private final ObjectMapper json = new ObjectMapper();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired EntityManagerFactory emf;
    @Autowired ConfigurableApplicationContext context;
    private Object securedService;
    private Method securedMethod;

    @DynamicPropertySource
    static void isolatedPostgres(DynamicPropertyRegistry registry) {
        if (database == null) database = new PostgresTestDatabase();
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
    }

    @BeforeAll
    void checksConfigurationAndFindsProtectedServiceWithoutInternalNameAssumptions() throws Exception {
        Properties properties = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input); properties.load(input);
        }
        Map.of("spring.datasource.url", "${COURSE_PG_URL}", "spring.datasource.username", "${COURSE_PG_USER}",
                "spring.datasource.password", "${COURSE_PG_PASSWORD}", "spring.jpa.hibernate.ddl-auto", "validate",
                "spring.jpa.open-in-view", "false").forEach((key, value) -> assertEquals(value, properties.getProperty(key), key));
        assertEquals("validate", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", context.getEnvironment().getProperty("spring.jpa.open-in-view"));
        assertNotNull(getClass().getClassLoader().getResource("db/migration/V1__create_bank_users.sql"));
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            assertEquals(database.schema(), connection.getSchema());
            try (var result = statement.executeQuery("SELECT id, username, password_hash, role FROM bank_users WHERE 1=0")) {
                int[] types = {Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}; int[] sizes = {0, 40, 100, 16};
                for (int i = 0; i < 4; i++) {
                    assertEquals(types[i], result.getMetaData().getColumnType(i+1));
                    assertEquals(java.sql.ResultSetMetaData.columnNoNulls, result.getMetaData().isNullable(i+1));
                    if (i > 0) assertEquals(sizes[i], result.getMetaData().getPrecision(i+1));
                }
                assertTrue(result.getMetaData().isAutoIncrement(1));
            }
        }
        var entity = emf.getMetamodel().getEntities().stream().filter(e -> e.getJavaType().isAnnotationPresent(Table.class)
                && "bank_users".equals(e.getJavaType().getAnnotation(Table.class).name())).findFirst().orElseThrow();
        assertEquals(Long.class, entity.getIdType().getJavaType());
        assertEquals("", entity.getJavaType().getAnnotation(Table.class).schema());
        Map<String, Integer> mappedLengths = new HashMap<>();
        for (var attribute : entity.getAttributes()) {
            if (attribute.getJavaMember() instanceof AnnotatedElement member) {
                Column column = member.getAnnotation(Column.class);
                if (column != null) mappedLengths.put(column.name().isEmpty() ? attribute.getName() : column.name(), column.length());
            }
        }
        Map.of("username", 40, "password_hash", 100, "role", 16)
                .forEach((column, size) -> assertEquals(size, mappedLengths.get(column), "Длина JPA-колонки " + column));
        var history = jdbc.queryForList("SELECT version,script,checksum,success FROM flyway_schema_history");
        assertEquals(1, history.size()); assertEquals("1", history.getFirst().get("version"));
        assertEquals("V1__create_bank_users.sql", history.getFirst().get("script"));
        assertNotNull(history.getFirst().get("checksum")); assertEquals(Boolean.TRUE, history.getFirst().get("success"));
        assertEquals(List.of(), stored(), "Не добавляй пользователей при запуске.");
        List<Map.Entry<Object, Method>> methods = new ArrayList<>();
        for (Object bean : context.getBeansWithAnnotation(Service.class).values()) {
            Class<?> type = AopUtils.getTargetClass(bean);
            for (Method method : type.getMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0
                        && (method.getReturnType() == long.class || method.getReturnType() == Long.class)
                        && AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    methods.add(Map.entry(bean, method));
                }
            }
        }
        assertEquals(1, methods.size(), "Создай один public метод @Service с @PreAuthorize, без параметров, возвращающий long/Long — количество клиентов.");
        securedService = methods.getFirst().getKey();
        securedMethod = securedService.getClass().getMethod(methods.getFirst().getValue().getName());
        assertTrue(AopUtils.isAopProxy(securedService), "Защищённый сервис должен вызываться через Spring bean/proxy.");
    }

    @BeforeEach
    void fixtures() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM bank_users");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('bank_users', 'id'), 100, false)");
        insert(11, "alex", CLIENT_PASSWORD, "CLIENT");
        insert(22, "nina", AUDITOR_PASSWORD, "AUDITOR");
        insert(3_000_000_000L, "sam", "Sam_88!", "CLIENT");
    }

    @AfterEach
    void clearsDirectInvocationAuthentication() { SecurityContextHolder.clearContext(); }

    @AfterAll
    void cleanup() throws Exception { if (database != null) database.close(); }

    @Test
    void publicInfoWorksForAnonymousAndBothRoles() throws Exception {
        request(get("/bank/info"), null, null, 200, Map.of("name", "Practice Bank"));
        request(get("/bank/info"), "alex", CLIENT_PASSWORD, 200, Map.of("name", "Practice Bank"));
        request(get("/bank/info"), "nina", AUDITOR_PASSWORD, 200, Map.of("name", "Practice Bank"));
    }

    @Test
    void protectedRoutesRejectMissingUnknownAndIncorrectCredentials() throws Exception {
        for (String path : List.of("/bank/me", "/bank/client-count", "/unmapped")) {
            request(get(path), null, null, 401, null);
        }
        for (String[] pair : new String[][]{{"unknown", "Unknown_88!"}, {"alex", "wrong"}, {"nina", "wrong"}, {"alex", ""}, {"Alex", CLIENT_PASSWORD}}) {
            for (String path : List.of("/bank/me", "/bank/client-count")) request(get(path), pair[0], pair[1], 401, null);
        }
        jdbc.update("DELETE FROM bank_users WHERE username='nina'");
        request(get("/bank/client-count"), "nina", AUDITOR_PASSWORD, 401, null);
    }

    @Test
    void profilesRemainAvailableToBothRolesAndPreserveBoundaryValues() throws Exception {
        profile("alex", CLIENT_PASSWORD, "CLIENT"); profile("nina", AUDITOR_PASSWORD, "AUDITOR");
        profile("sam", "Sam_88!", "CLIENT");
        insert(Long.MAX_VALUE, "П".repeat(40), "Unicode_88!", "CLIENT");
        insert(41, "x", "Short_88!", "AUDITOR");
        insert(42, " Олег \"Тест\" ", "Space_88!", "CLIENT");
        profile("П".repeat(40), "Unicode_88!", "CLIENT"); profile("x", "Short_88!", "AUDITOR");
        profile(" Олег \"Тест\" ", "Space_88!", "CLIENT");
        request(get("/bank/me").param("username", "nina"), "alex", CLIENT_PASSWORD, 200, Map.of("username", "alex", "role", "CLIENT"));
    }

    @Test
    void auditorReadsCurrentClientCountIncludingZeroWhileClientIsDenied() throws Exception {
        request(get("/bank/client-count"), "nina", AUDITOR_PASSWORD, 200, Map.of("count", 2));
        request(get("/bank/client-count"), "alex", CLIENT_PASSWORD, 403, null);
        insert(44, "extra", "Extra_88!", "CLIENT"); insert(55, "staff", "Staff_88!", "AUDITOR");
        request(get("/bank/client-count"), "nina", AUDITOR_PASSWORD, 200, Map.of("count", 3));
        jdbc.update("DELETE FROM bank_users WHERE role='CLIENT'");
        request(get("/bank/client-count"), "nina", AUDITOR_PASSWORD, 200, Map.of("count", 0));
        insert(56, "last", "Last_88!", "CLIENT");
        request(get("/bank/client-count"), "last", "Last_88!", 403, null);
    }

    @Test
    void databaseRoleAndPasswordChangesAffectNextHttpRequest() throws Exception {
        jdbc.update("UPDATE bank_users SET role='AUDITOR' WHERE username='alex'");
        request(get("/bank/client-count"), "alex", CLIENT_PASSWORD, 200, Map.of("count", 1));
        jdbc.update("UPDATE bank_users SET role='CLIENT' WHERE username='nina'");
        request(get("/bank/client-count"), "nina", AUDITOR_PASSWORD, 403, null);
        profile("nina", AUDITOR_PASSWORD, "CLIENT");
        jdbc.update("UPDATE bank_users SET password_hash=? WHERE username='alex'", HASHES.encode("New_88!"));
        request(get("/bank/client-count"), "alex", CLIENT_PASSWORD, 401, null);
        request(get("/bank/client-count"), "alex", "New_88!", 200, Map.of("count", 2));
    }

    @Test
    void directServiceCallRejectsMissingAuthenticationAndClientRole() {
        var before = stored();
        Throwable unauthenticated = assertThrows(Throwable.class, this::callService);
        assertTrue(unauthenticated instanceof AuthenticationException || unauthenticated instanceof AccessDeniedException,
                "Без аутентификации прямой вызов Spring-сервиса должен быть отклонён механизмом безопасности.");
        authenticate("alex", "CLIENT");
        assertThrows(AccessDeniedException.class, this::callService);
        assertEquals(before, stored());
    }

    @Test
    void directAuditorServiceCallReadsDatabaseAndWorksWithoutHttp() throws Throwable {
        authenticate("nina", "AUDITOR");
        var before = stored(); assertEquals(2L, callService()); assertEquals(before, stored());
        insert(100, "added", "Added_88!", "CLIENT");
        before = stored(); assertEquals(3L, callService()); assertEquals(before, stored());
        jdbc.update("DELETE FROM bank_users WHERE role='CLIENT'");
        before = stored(); assertEquals(0L, callService()); assertEquals(before, stored());
    }

    @Test
    void postgresRejectsInvalidRowsAndDuplicateIdAndGeneratesIdentity() {
        String hash = HASHES.encode("Constraints_88!");
        Object[][] invalid = {{"alex", hash, "CLIENT"}, {null, hash, "CLIENT"}, {"", hash, "CLIENT"},
                {"L".repeat(41), hash, "CLIENT"}, {"valid", null, "CLIENT"}, {"valid", "H".repeat(101), "CLIENT"},
                {"valid", hash, null}, {"valid", hash, "ADMIN"}, {"valid", hash, "client"}, {"valid", hash, ""}};
        var before = stored();
        for (Object[] row : invalid) {
            assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO bank_users(username,password_hash,role) VALUES (?,?,?)", row));
            assertEquals(before, stored());
        }
        assertThrows(DataAccessException.class, () -> insert(11, "duplicate", "Id_88!", "CLIENT"));
        assertEquals(before, stored());
        jdbc.update("INSERT INTO bank_users(username,password_hash,role) VALUES (?,?,?)", "generated", hash, "CLIENT");
        assertTrue(jdbc.queryForObject("SELECT id FROM bank_users WHERE username='generated'", Long.class) > 0);
    }

    private void authenticate(String login, String role) {
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(new UsernamePasswordAuthenticationToken(login, "test-only", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        SecurityContextHolder.setContext(security);
    }

    private long callService() throws Throwable {
        try { return ((Number) securedMethod.invoke(securedService)).longValue(); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }

    private void request(MockHttpServletRequestBuilder request, String login, String password, int expected, Object body) throws Exception {
        var before = stored();
        if (login != null) request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((login + ":" + password).getBytes(StandardCharsets.UTF_8)));
        var result = mvc.perform(request).andExpect(status().is(expected));
        if (expected == 401) result.andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Basic")));
        if (body != null) result.andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json(json.writeValueAsString(body), JsonCompareMode.STRICT));
        assertEquals(before, stored(), "Запросы и отказ в доступе не изменяют строки.");
    }

    private void profile(String login, String password, String role) throws Exception {
        request(get("/bank/me"), login, password, 200, Map.of("username", login, "role", role));
    }

    private void insert(long id, String login, String password, String role) {
        jdbc.update("INSERT INTO bank_users(id,username,password_hash,role) VALUES (?,?,?,?)", id, login, HASHES.encode(password), role);
    }

    private List<Map<String, Object>> stored() { return jdbc.queryForList("SELECT id,username,password_hash,role FROM bank_users ORDER BY id"); }
}
