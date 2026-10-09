package learning.task089;

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
class BankProfileAccessApiTest {
    private static final String CLIENT_PASSWORD = "Client_89!";
    private static final String AUDITOR_PASSWORD = "Audit_89!";
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
                if (Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class
                        && method.getReturnType() != void.class
                        && AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    methods.add(Map.entry(bean, method));
                }
            }
        }
        assertEquals(1, methods.size(), "Создай один public метод @Service с @PreAuthorize и единственным String-параметром — логином запрошенного профиля. Он возвращает DTO с username и role.");
        securedService = methods.getFirst().getKey();
        securedMethod = securedService.getClass().getMethod(methods.getFirst().getValue().getName(), String.class);
        assertTrue(AopUtils.isAopProxy(securedService), "Защищённый сервис должен вызываться через Spring bean/proxy.");
    }

    @BeforeEach
    void fixtures() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM bank_users");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('bank_users', 'id'), 100, false)");
        insert(11, "alex", CLIENT_PASSWORD, "CLIENT");
        insert(22, "nina", AUDITOR_PASSWORD, "AUDITOR");
        insert(3_000_000_000L, "sam", "Sam_89!", "CLIENT");
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
    void protectedRoutesRequireValidCurrentCredentials() throws Exception {
        for (String target : List.of("alex", "nina", "missing")) {
            request(path(target), null, null, 401, null);
            request(path(target), "unknown", "Unknown_89!", 401, null);
            request(path(target), "alex", "wrong", 401, null);
            request(path(target), "Alex", CLIENT_PASSWORD, 401, null);
            request(path(target), "alex", "", 401, null);
        }
        request(get("/unmapped"), null, null, 401, null);
        jdbc.update("DELETE FROM bank_users WHERE username='sam'");
        request(path("alex"), "sam", "Sam_89!", 401, null);
    }

    @Test
    void userDetailsServiceExplicitlyReportsMissingUsersAndReadsCurrentHashAndRole() {
        var users = context.getBean(org.springframework.security.core.userdetails.UserDetailsService.class);
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> users.loadUserByUsername("missing"));
        var details = users.loadUserByUsername("alex");
        assertEquals("alex", details.getUsername());
        assertEquals(jdbc.queryForObject("SELECT password_hash FROM bank_users WHERE username='alex'", String.class), details.getPassword());
        assertEquals(Set.of("ROLE_CLIENT"), details.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()));
        jdbc.update("UPDATE bank_users SET role='AUDITOR', password_hash=? WHERE username='alex'", HASHES.encode("Changed_89!"));
        details = users.loadUserByUsername("alex");
        assertTrue(HASHES.matches("Changed_89!", details.getPassword()));
        assertEquals(Set.of("ROLE_AUDITOR"), details.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()));
        jdbc.update("DELETE FROM bank_users WHERE username='alex'");
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> users.loadUserByUsername("alex"));
    }

    @Test
    void clientReadsOwnProfileAndCannotChooseAnotherOwnerUsingQueryParameters() throws Exception {
        profile("alex", CLIENT_PASSWORD, "alex", "CLIENT");
        profile("sam", "Sam_89!", "sam", "CLIENT");
        request(path("alex").param("username", "nina").param("role", "AUDITOR"),
                "alex", CLIENT_PASSWORD, 200, Map.of("username", "alex", "role", "CLIENT"));
        request(path("nina").param("username", "alex").param("role", "AUDITOR"), "alex", CLIENT_PASSWORD, 403, null);
    }

    @Test
    void clientCannotReadOtherClientsAuditorsOrMissingProfiles() throws Exception {
        for (String target : List.of("sam", "nina", "missing", "Alex", "alexx")) {
            request(path(target), "alex", CLIENT_PASSWORD, 403, null);
        }
        request(path("alex"), "sam", "Sam_89!", 403, null);
        // A request for a non-existing foreign login is still forbidden: permission is checked first.
        jdbc.update("DELETE FROM bank_users WHERE username='sam'");
        request(path("sam"), "alex", CLIENT_PASSWORD, 403, null);
    }

    @Test
    void auditorReadsAnyExistingProfileAndGets404ForMissingProfile() throws Exception {
        profile("nina", AUDITOR_PASSWORD, "alex", "CLIENT");
        profile("nina", AUDITOR_PASSWORD, "sam", "CLIENT");
        profile("nina", AUDITOR_PASSWORD, "nina", "AUDITOR");
        request(path("missing"), "nina", AUDITOR_PASSWORD, 404, null);
        jdbc.update("DELETE FROM bank_users WHERE username='alex'");
        request(path("alex"), "nina", AUDITOR_PASSWORD, 404, null);
    }

    @Test
    void profilesPreserveLargeIdsUnicodeAndBoundaryLoginsWithoutExtraJsonFields() throws Exception {
        insert(Long.MAX_VALUE, "П".repeat(40), "Unicode_89!", "CLIENT");
        insert(41, "x", "Short_89!", "CLIENT");
        insert(42, " Олег \"Тест\" ", "Space_89!", "CLIENT");
        insert(43, "Mixed.Case", "Mixed_89!", "CLIENT");
        for (String[] pair : new String[][]{{"П".repeat(40), "Unicode_89!"}, {"x", "Short_89!"},
                {" Олег \"Тест\" ", "Space_89!"}, {"Mixed.Case", "Mixed_89!"}}) {
            profile(pair[0], pair[1], pair[0], "CLIENT");
            profile("nina", AUDITOR_PASSWORD, pair[0], "CLIENT");
        }
        request(path("mixed.case"), "Mixed.Case", "Mixed_89!", 403, null);
    }

    @Test
    void roleAndPasswordChangesAffectNextHttpRequestAndProfileReadsAreCurrent() throws Exception {
        request(path("sam"), "alex", CLIENT_PASSWORD, 403, null);
        jdbc.update("UPDATE bank_users SET role='AUDITOR' WHERE username='alex'");
        profile("alex", CLIENT_PASSWORD, "sam", "CLIENT");
        profile("alex", CLIENT_PASSWORD, "alex", "AUDITOR");
        jdbc.update("UPDATE bank_users SET role='AUDITOR' WHERE username='sam'");
        profile("nina", AUDITOR_PASSWORD, "sam", "AUDITOR");
        jdbc.update("UPDATE bank_users SET role='CLIENT' WHERE username='alex'");
        request(path("sam"), "alex", CLIENT_PASSWORD, 403, null);
        profile("alex", CLIENT_PASSWORD, "alex", "CLIENT");
        jdbc.update("UPDATE bank_users SET password_hash=? WHERE username='alex'", HASHES.encode("New_89!"));
        request(path("alex"), "alex", CLIENT_PASSWORD, 401, null);
        profile("alex", "New_89!", "alex", "CLIENT");
    }

    @Test
    void directServiceCallRejectsUnauthenticatedAndForeignProfileAccess() {
        var before = stored();
        Throwable unauthenticated = assertThrows(Throwable.class, () -> callService("alex"));
        assertTrue(isSecurityFailure(unauthenticated),
                "Прямой вызов без аутентификации должен отклоняться механизмом безопасности.");
        authenticate("alex", "CLIENT");
        for (String target : List.of("sam", "nina", "missing", "Alex")) {
            assertThrows(AccessDeniedException.class, () -> callService(target), "Защита должна работать на сервисе для " + target);
        }
        assertEquals(before, stored());
    }

    @Test
    void directOwnerCallReturnsDtoAndRechecksArgumentEveryTime() throws Throwable {
        authenticate("alex", "CLIENT");
        directProfile("alex", "CLIENT");
        assertThrows(AccessDeniedException.class, () -> callService("sam"));
        directProfile("alex", "CLIENT");
        authenticate("sam", "CLIENT");
        directProfile("sam", "CLIENT");
        assertThrows(AccessDeniedException.class, () -> callService("alex"));
    }

    @Test
    void directAuditorCallReadsCurrentDatabaseWithoutHttp() throws Throwable {
        authenticate("nina", "AUDITOR");
        directProfile("alex", "CLIENT"); directProfile("nina", "AUDITOR");
        jdbc.update("UPDATE bank_users SET role='AUDITOR' WHERE username='alex'");
        directProfile("alex", "AUDITOR");
        jdbc.update("DELETE FROM bank_users WHERE username='sam'");
        var before = stored();
        Throwable missing = assertThrows(Throwable.class, () -> callService("sam"));
        assertFalse(isSecurityFailure(missing),
                "Аудитору разрешён поиск: отсутствующий профиль — ошибка отсутствия данных, а не отказ в доступе.");
        assertFalse(missing instanceof NullPointerException || missing instanceof NoSuchElementException,
                "Обработай отсутствие строки явно своим исключением или Spring HTTP-исключением для 404.");
        assertEquals(before, stored());
    }

    @Test
    void postgresRejectsInvalidRowsAndDuplicateIdAndGeneratesIdentity() {
        String hash = HASHES.encode("Constraints_89!");
        Object[][] invalid = {{"alex", hash, "CLIENT"}, {null, hash, "CLIENT"}, {"", hash, "CLIENT"},
                {"L".repeat(41), hash, "CLIENT"}, {"valid", null, "CLIENT"}, {"valid", "H".repeat(101), "CLIENT"},
                {"valid", hash, null}, {"valid", hash, "ADMIN"}, {"valid", hash, "client"}, {"valid", hash, ""}};
        var before = stored();
        for (Object[] row : invalid) {
            assertThrows(DataAccessException.class, () -> jdbc.update("INSERT INTO bank_users(username,password_hash,role) VALUES (?,?,?)", row));
            assertEquals(before, stored());
        }
        assertThrows(DataAccessException.class, () -> insert(11, "duplicate", "Id_89!", "CLIENT"));
        assertEquals(before, stored());
        jdbc.update("INSERT INTO bank_users(username,password_hash,role) VALUES (?,?,?)", "generated", hash, "CLIENT");
        assertTrue(jdbc.queryForObject("SELECT id FROM bank_users WHERE username='generated'", Long.class) > 0);
    }

    private boolean isSecurityFailure(Throwable failure) {
        // SpEL property access can wrap an AuthenticationCredentialsNotFoundException.
        // Accept that specific security cause, not an arbitrary evaluation/runtime error.
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof AuthenticationException || cause instanceof AccessDeniedException) return true;
        }
        return false;
    }

    private void authenticate(String login, String role) {
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(new UsernamePasswordAuthenticationToken(login, "test-only", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        SecurityContextHolder.setContext(security);
    }

    private Object callService(String target) throws Throwable {
        try { return securedMethod.invoke(securedService, target); }
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

    private MockHttpServletRequestBuilder path(String target) { return get("/bank/users/{username}", target); }

    private void profile(String login, String password, String target, String role) throws Exception {
        request(path(target), login, password, 200, Map.of("username", target, "role", role));
    }

    private void directProfile(String target, String role) throws Throwable {
        var before = stored();
        assertEquals(json.valueToTree(Map.of("username", target, "role", role)), json.valueToTree(callService(target)),
                "Сервис должен вернуть DTO только с username и role.");
        assertEquals(before, stored());
    }

    private void insert(long id, String login, String password, String role) {
        jdbc.update("INSERT INTO bank_users(id,username,password_hash,role) VALUES (?,?,?,?)", id, login, HASHES.encode(password), role);
    }

    private List<Map<String, Object>> stored() { return jdbc.queryForList("SELECT id,username,password_hash,role FROM bank_users ORDER BY id"); }
}
