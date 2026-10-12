package learning.task096;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Acceptance tests owned by the trainer; the learner creates the inherited database setup. */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BankExpenseApiTest extends BankPostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired DataSource dataSource;
    @Autowired ObjectMapper json;
    @Autowired Environment environment;
    private static String firstContainerId;
    private JdbcTemplate sql;

    @BeforeEach
    void resetOnlyTheDisposableTestDatabase() throws Exception {
        PostgreSQLContainer pg = container();
        assertTrue(pg.isRunning(), "Тестовая конфигурация должна запустить PostgreSQL через @Container.");
        if (firstContainerId == null) firstContainerId = pg.getContainerId();
        assertEquals(firstContainerId, pg.getContainerId(), "Используй один static-контейнер на класс тестов.");
        try (var connection = dataSource.getConnection()) {
            assertEquals(pg.getJdbcUrl(), connection.getMetaData().getURL(), "Приложение подключено к контейнеру теста.");
            assertEquals(pg.getUsername(), connection.getMetaData().getUserName());
        }
        sql = new JdbcTemplate(dataSource);
        sql.execute("TRUNCATE expense_entries RESTART IDENTITY");
    }

    @Test
    void learnerSetupUsesStaticContainerAndDynamicSpringProperties() throws Exception {
        Class<?> support = BankPostgresTestSupport.class;
        assertTrue(Modifier.isAbstract(support.getModifiers()));
        assertNotNull(support.getAnnotation(SpringBootTest.class));
        Testcontainers settings = support.getAnnotation(Testcontainers.class); assertNotNull(settings);
        assertFalse(settings.disabledWithoutDocker(), "Отсутствие Docker должно давать ошибку, а не пропуск.");
        var methods = Arrays.stream(support.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DynamicPropertySource.class)).toList();
        assertEquals(1, methods.size());
        assertTrue(Modifier.isStatic(methods.getFirst().getModifiers()));
        assertArrayEquals(new Class<?>[]{DynamicPropertyRegistry.class}, methods.getFirst().getParameterTypes());
        assertEquals(container().getJdbcUrl(), environment.getProperty("spring.datasource.url"));
        assertEquals(container().getUsername(), environment.getProperty("spring.datasource.username"));
        assertEquals(container().getPassword(), environment.getProperty("spring.datasource.password"));
        assertTrue(sql.queryForObject("SELECT version()", String.class).startsWith("PostgreSQL 17"));
        assertEquals("postgres:17-alpine", container().getDockerImageName().replace("docker.io/library/", ""));
        assertNull(support.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    void startsWithEmptyTableAndOneSuccessfulFlywayV1() {
        assertEquals(0L, rows());
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success AND script='V1__create_expense_entries.sql'", Integer.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version IS NOT NULL", Integer.class));
        assertEquals("YES", sql.queryForObject("SELECT is_identity FROM information_schema.columns WHERE table_schema='public' AND table_name='expense_entries' AND column_name='id'", String.class));
        assertEquals("bigint", sql.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_schema='public' AND table_name='expense_entries' AND column_name='amount_minor'", String.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='expense_entries' AND constraint_type='PRIMARY KEY'", Integer.class));
        boolean jpa = org.springframework.boot.SpringApplication.class.getClassLoader()
                .getResource("jakarta/persistence/Entity.class") != null;
        if (jpa) {
            assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
        }
    }

    @Test
    void createsExpenseAndReadsTheActualSavedRow() throws Exception {
        long id = create("Coffee", 250);
        assertTrue(id > 0);
        check(getById(id, 200), id, "Coffee", 250);
        assertEquals("Coffee", sql.queryForObject("SELECT description FROM expense_entries WHERE id=?", String.class, id));
        assertEquals(250L, sql.queryForObject("SELECT amount_minor FROM expense_entries WHERE id=?", Long.class, id));
        assertEquals(1L, rows());
    }

    @Test
    void createsSeparateRowsWithIndependentGeneratedIds() throws Exception {
        long a = create("Subscription", 499);
        long b = create("Subscription", 499);
        assertNotEquals(a, b);
        check(getById(a, 200), a, "Subscription", 499);
        check(getById(b, 200), b, "Subscription", 499);
        assertEquals(2L, rows());
    }

    @Test
    void rejectsMissingNullBlankAndOverlongDescriptionWithoutInserts() throws Exception {
        for (String body : List.of("{\"amountMinor\":1}", "{\"description\":null,\"amountMinor\":1}",
                "{\"description\":\"\",\"amountMinor\":1}", "{\"description\":\"   \",\"amountMinor\":1}",
                "{\"description\":\"\\t\\n\",\"amountMinor\":1}")) {
            postBody(body, 400);
        }
        postBody(json.writeValueAsString(java.util.Map.of("description", "a".repeat(101), "amountMinor", 1)), 400);
        assertEquals(0L, rows());
    }

    @Test
    void rejectsMissingNullNonpositiveAndOverflowingAmountWithoutInserts() throws Exception {
        for (String body : List.of("{\"description\":\"Rent\"}", "{\"description\":\"Rent\",\"amountMinor\":null}",
                "{\"description\":\"Rent\",\"amountMinor\":0}", "{\"description\":\"Rent\",\"amountMinor\":-1}",
                "{\"description\":\"Rent\",\"amountMinor\":-9223372036854775808}",
                "{\"description\":\"Rent\",\"amountMinor\":9223372036854775808}")) {
            postBody(body, 400);
        }
        assertEquals(0L, rows());
    }

    @Test
    void preservesBoundaryLengthsAndAmountsBeyondInt() throws Exception {
        for (long amount : new long[]{1, 2_147_483_648L, Long.MAX_VALUE}) {
            String description = "x".repeat(100);
            long id = create(description, amount);
            check(getById(id, 200), id, description, amount);
        }
        assertEquals(3L, rows());
    }

    @Test
    void preservesUnicodeAndSignificantSpaces() throws Exception {
        String description = "  Оплата обучения №1  ";
        long id = create(description, 12500);
        check(getById(id, 200), id, description, 12500);
        assertEquals(description, sql.queryForObject("SELECT description FROM expense_entries WHERE id=?", String.class, id));
    }

    @Test
    void readsCurrentDatabaseValuesAndReturns404AfterDelete() throws Exception {
        long id = create("Original", 10);
        sql.update("UPDATE expense_entries SET description=?, amount_minor=? WHERE id=?", "Updated", 999L, id);
        check(getById(id, 200), id, "Updated", 999);
        sql.update("DELETE FROM expense_entries WHERE id=?", id);
        getById(id, 404);
    }

    @Test
    void readsLargeExistingIdsAndDistinguishesInvalidFromMissingIds() throws Exception {
        long id = 3_000_000_001L;
        sql.update("INSERT INTO expense_entries(id,description,amount_minor) VALUES (?,?,?)", id, "Imported", 55L);
        check(getById(id, 200), id, "Imported", 55);
        getById(Long.MAX_VALUE, 404);
        getById(0, 400); getById(-1, 400);
        assertEquals(400, mvc.perform(get("/bank/expenses/not-a-number")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/bank/expenses/9223372036854775808")).andReturn().getResponse().getStatus());
    }

    @Test
    void malformedAndMissingJsonDoNotWriteRows() throws Exception {
        postBody("{", 400); postBody("null", 400); postBody("", 400);
        assertEquals(0L, rows());
    }

    @Test
    void migrationProtectsDataFromInvalidDirectSql() {
        for (String statement : List.of(
                "INSERT INTO expense_entries(description,amount_minor) VALUES (NULL,1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('',1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('   ',1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('" + "a".repeat(101) + "',1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',NULL)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',0)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',-1)")) {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> sql.update(statement));
        }
        assertEquals(0L, rows());
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('Valid',1)");
        assertEquals(1L, rows());
    }

    private PostgreSQLContainer container() throws Exception {
        var fields = Arrays.stream(BankPostgresTestSupport.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Container.class) && PostgreSQLContainer.class.isAssignableFrom(f.getType())).toList();
        assertEquals(1, fields.size(), "Объяви один PostgreSQLContainer с @Container в тестовой базе.");
        var field = fields.getFirst(); assertTrue(Modifier.isStatic(field.getModifiers()));
        field.setAccessible(true); return (PostgreSQLContainer) field.get(null);
    }
    private long rows() { return sql.queryForObject("SELECT count(*) FROM expense_entries", Long.class); }
    private MvcResult postBody(String body, int expected) throws Exception {
        MvcResult result = mvc.perform(post("/bank/expenses").contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        assertEquals(expected, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return result;
    }
    private long create(String description, long amount) throws Exception {
        MvcResult result = postBody(json.writeValueAsString(java.util.Map.of("description", description, "amountMinor", amount)), 201);
        JsonNode body = readJson(result);
        long id = body.path("id").asLong(); check(body, id, description, amount);
        assertTrue(id > 0);
        assertEquals("/bank/expenses/" + id, result.getResponse().getHeader("Location"));
        return id;
    }
    private JsonNode getById(long id, int status) throws Exception {
        var result = mvc.perform(get("/bank/expenses/" + id)).andReturn();
        assertEquals(status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        return status == 200 ? readJson(result) : null;
    }
    private JsonNode readJson(MvcResult result) throws Exception {
        assertNotNull(result.getResponse().getContentType());
        assertTrue(result.getResponse().getContentType().startsWith("application/json"));
        return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    private void check(JsonNode body, long id, String description, long amount) {
        assertEquals(3, body.size());
        assertTrue(body.path("id").isIntegralNumber()); assertEquals(id, body.path("id").longValue());
        assertEquals(description, body.path("description").asText());
        assertTrue(body.path("amountMinor").isIntegralNumber()); assertEquals(amount, body.path("amountMinor").longValue());
    }
}
