package learning.task097;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BankHealthApiTest extends BankPostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DataSource source;
    @Autowired Environment environment;
    @Autowired HealthEndpoint endpoint;
    @Autowired HealthEndpointGroups groups;
    @Autowired HealthContributorRegistry contributors;
    private JdbcTemplate sql;

    @BeforeEach
    void cleanOnlyThisTestDatabase() throws Exception {
        PostgreSQLContainer pg = container(); assertTrue(pg.isRunning());
        try (var c = source.getConnection()) { assertEquals(pg.getJdbcUrl(), c.getMetaData().getURL()); }
        sql = new JdbcTemplate(source);
        sql.execute("TRUNCATE expense_entries RESTART IDENTITY");
    }

    @Test
    void usesStandardActuatorDatabaseIndicatorAndSeparateProbeGroups() throws Exception {
        PostgreSQLContainer pg = container();
        assertNotNull(BankPostgresTestSupport.class.getAnnotation(SpringBootTest.class));
        var settings = BankPostgresTestSupport.class.getAnnotation(Testcontainers.class); assertNotNull(settings);
        assertFalse(settings.disabledWithoutDocker());
        var methods = Arrays.stream(BankPostgresTestSupport.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DynamicPropertySource.class)).toList();
        assertEquals(1, methods.size()); assertTrue(Modifier.isStatic(methods.getFirst().getModifiers()));
        assertArrayEquals(new Class<?>[]{DynamicPropertyRegistry.class}, methods.getFirst().getParameterTypes());
        assertEquals(pg.getJdbcUrl(), environment.getProperty("spring.datasource.url"));
        assertEquals(pg.getUsername(), environment.getProperty("spring.datasource.username"));
        assertEquals(pg.getPassword(), environment.getProperty("spring.datasource.password"));
        assertEquals("postgres:17-alpine", pg.getDockerImageName().replace("docker.io/library/", ""));
        if (org.springframework.boot.SpringApplication.class.getClassLoader().getResource("jakarta/persistence/Entity.class") != null) {
            assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
        }
        assertNotNull(endpoint);
        assertInstanceOf(DataSourceHealthIndicator.class, contributors.getContributor("db"));
        assertNotNull(groups.get("readiness")); assertNotNull(groups.get("liveness"));
        assertTrue(groups.get("readiness").isMember("readinessState"));
        assertTrue(groups.get("readiness").isMember("db"), "В нашем API база обязательна для готовности.");
        assertTrue(groups.get("liveness").isMember("livenessState"));
        assertFalse(groups.get("liveness").isMember("db"), "Недоступность базы не делает JVM приложения сломанной.");
        HikariDataSource pool = assertInstanceOf(HikariDataSource.class, source);
        assertTrue(pool.getConnectionTimeout() >= 250 && pool.getConnectionTimeout() <= 2000,
                "Ограничь connection-timeout Hikari до 250..2000 мс для этой проверки.");
    }

    @Test
    void healthAndBothProbesAreUpAfterStartup() throws Exception {
        for (String p : List.of("/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness")) health(p, 200, "UP");
    }

    @Test
    void healthResponsesDoNotExposeDetailsComponentsOrDatabaseSettings() throws Exception {
        for (String p : List.of("/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness")) {
            health(p + "?show-details=always", 200, "UP");
        }
    }

    @Test
    void exposesOnlyHealthManagementEndpoint() throws Exception {
        for (String p : List.of("env", "beans", "mappings", "metrics", "info", "configprops")) {
            assertEquals(404, mvc.perform(get("/actuator/" + p)).andReturn().getResponse().getStatus());
        }
    }

    @Test
    void emptyDatabaseReturnsZeroAndHasOneFlywayVersion() throws Exception {
        count(0);
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success AND script='V1__create_expense_entries.sql'", Integer.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version IS NOT NULL", Integer.class));
        assertTrue(sql.queryForObject("SELECT version()", String.class).startsWith("PostgreSQL 17"));
    }

    @Test
    void counterReflectsCurrentInsertsAndDeletes() throws Exception {
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',100),('Travel',200)"); count(2);
        sql.update("DELETE FROM expense_entries WHERE description='Rent'"); count(1);
        sql.update("DELETE FROM expense_entries"); count(0);
    }

    @Test
    void manyRowsHaveAnIntegerCounterWithoutExtraFields() throws Exception {
        sql.update("INSERT INTO expense_entries(description,amount_minor) SELECT 'Entry ' || n,n FROM generate_series(1,125) n"); count(125);
    }

    @Test
    void databaseOutageMakesReadinessDownButLivenessUpAndRecoveryPreservesData() throws Exception {
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('Keep this row',10)");
        String id = container().getContainerId();
        PostgreSQLContainer pg = container();
        String role = "\"" + pg.getUsername().replace("\"", "\"\"") + "\"";
        // Keep one administrative connection alive while temporarily blocking new application logins.
        // This preserves the Docker mapping and tests health against the same actual PostgreSQL.
        try (var admin = DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
             var statement = admin.createStatement()) {
            boolean blocked = false;
            try {
                statement.execute("ALTER ROLE " + role + " NOLOGIN"); blocked = true;
                statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE usename=current_user AND pid<>pg_backend_pid()");
                assertInstanceOf(HikariDataSource.class, source).getHikariPoolMXBean().softEvictConnections();
                health("/actuator/health", 503, "DOWN");
                health("/actuator/health/readiness", 503, "DOWN");
                health("/actuator/health/liveness", 200, "UP");
            } finally {
                if (blocked) statement.execute("ALTER ROLE " + role + " LOGIN");
            }
        }
        waitForDatabase();
        health("/actuator/health", 200, "UP");
        health("/actuator/health/readiness", 200, "UP");
        health("/actuator/health/liveness", 200, "UP");
        count(1);
        assertEquals(id, container().getContainerId(), "Используем тот же тестовый контейнер без перезапуска Spring.");
        assertEquals("Keep this row", sql.queryForObject("SELECT description FROM expense_entries", String.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success", Integer.class));
    }

    @Test
    void missingBusinessRouteIs404AndPostToCounterIs405() throws Exception {
        assertEquals(404, mvc.perform(get("/bank/missing")).andReturn().getResponse().getStatus());
        assertEquals(405, mvc.perform(post("/bank/expenses/count")).andReturn().getResponse().getStatus());
        count(0);
    }

    @Test
    void migrationKeepsIdentityAndRejectsInvalidDirectSql() {
        assertEquals("YES", sql.queryForObject("SELECT is_identity FROM information_schema.columns WHERE table_schema='public' AND table_name='expense_entries' AND column_name='id'", String.class));
        assertEquals("bigint", sql.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_schema='public' AND table_name='expense_entries' AND column_name='amount_minor'", String.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='expense_entries' AND constraint_type='PRIMARY KEY'", Integer.class));
        for (String statement : List.of("INSERT INTO expense_entries(description,amount_minor) VALUES (NULL,1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('',1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('   ',1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('" + "a".repeat(101) + "',1)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',NULL)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',0)",
                "INSERT INTO expense_entries(description,amount_minor) VALUES ('Rent',-1)")) {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> sql.update(statement));
        }
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES (?,?)", "a".repeat(100), Long.MAX_VALUE);
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM expense_entries", Long.class));
    }

    private void health(String path, int code, String expected) throws Exception {
        var response = mvc.perform(get(path)).andReturn().getResponse();
        assertEquals(code, response.getStatus(), path + ": " + response.getContentAsString());
        assertNotNull(response.getContentType()); assertTrue(response.getContentType().startsWith("application/json") || response.getContentType().contains("+json"));
        JsonNode body = json.readTree(response.getContentAsString());
        boolean root = path.split("\\?", 2)[0].equals("/actuator/health");
        if (root && body.has("groups")) {
            assertEquals(2, body.size(), "Допустимы status и стандартный список groups.");
            assertTrue(body.path("groups").isArray());
            java.util.Set<String> names = new java.util.HashSet<>();
            body.path("groups").forEach(group -> names.add(group.asText()));
            assertEquals(java.util.Set.of("liveness", "readiness"), names);
        } else assertEquals(1, body.size(), "В probe-ответе только status.");
        assertFalse(body.has("components")); assertFalse(body.has("details"));
        assertEquals(expected, body.path("status").asText());
    }
    private void count(long expected) throws Exception {
        var response = mvc.perform(get("/bank/expenses/count")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getContentType()); assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = json.readTree(response.getContentAsString());
        assertEquals(1, body.size()); assertTrue(body.path("count").isIntegralNumber());
        assertEquals(expected, body.path("count").longValue());
    }
    private PostgreSQLContainer container() throws Exception {
        var fields = Arrays.stream(BankPostgresTestSupport.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Container.class) && PostgreSQLContainer.class.isAssignableFrom(f.getType())).toList();
        assertEquals(1, fields.size()); var field = fields.getFirst(); assertTrue(Modifier.isStatic(field.getModifiers()));
        field.setAccessible(true); return (PostgreSQLContainer) field.get(null);
    }
    private void waitForDatabase() throws Exception {
        PostgreSQLContainer pg = container();
        long end = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < end) {
            try (var c = source.getConnection(); var s = c.createStatement()) {
                s.execute("SELECT 1"); return;
            } catch (java.sql.SQLException starting) { Thread.sleep(200); }
        }
        fail("Тестовый PostgreSQL не восстановился за 30 секунд.");
    }
}
