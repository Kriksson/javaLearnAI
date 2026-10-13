package learning.task098;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class BankExpenseLoggingApiTest extends BankPostgresTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DataSource source;
    @Autowired Environment environment;
    private JdbcTemplate sql;
    private Logger packageLogger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> capture;

    @BeforeEach
    void captureApplicationLogsAndCleanOnlyTestDatabase() {
        sql = new JdbcTemplate(source);
        sql.execute("TRUNCATE expense_entries RESTART IDENTITY");
        packageLogger = (Logger) LoggerFactory.getLogger("learning.task098");
        previousLevel = packageLogger.getLevel();
        capture = new ListAppender<>();
        capture.setContext(packageLogger.getLoggerContext());
        capture.start();
        packageLogger.addAppender(capture);
    }

    @AfterEach
    void detachCaptureAndRestoreLevel() {
        if (packageLogger != null) {
            packageLogger.detachAppender(capture);
            packageLogger.setLevel(previousLevel);
        }
        if (capture != null) capture.stop();
    }

    @Test
    void testDatabaseAndFlywayAreConfiguredWithoutDisablingDocker() throws Exception {
        var config = BankPostgresTestSupport.class;
        assertNotNull(config.getAnnotation(SpringBootTest.class));
        var lifecycle = config.getAnnotation(Testcontainers.class);
        assertNotNull(lifecycle);
        assertFalse(lifecycle.disabledWithoutDocker());
        var fields = Arrays.stream(config.getDeclaredFields()).filter(f ->
                f.isAnnotationPresent(Container.class) && PostgreSQLContainer.class.isAssignableFrom(f.getType())).toList();
        assertEquals(1, fields.size());
        var field = fields.getFirst();
        assertTrue(Modifier.isStatic(field.getModifiers()));
        field.setAccessible(true);
        PostgreSQLContainer pg = (PostgreSQLContainer) field.get(null);
        assertTrue(pg.isRunning());
        assertEquals("postgres:17-alpine", pg.getDockerImageName().replace("docker.io/library/", ""));
        assertEquals(pg.getJdbcUrl(), environment.getProperty("spring.datasource.url"));
        assertEquals(pg.getUsername(), environment.getProperty("spring.datasource.username"));
        assertEquals(pg.getPassword(), environment.getProperty("spring.datasource.password"));
        var dynamic = Arrays.stream(config.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(DynamicPropertySource.class)).toList();
        assertEquals(1, dynamic.size());
        assertTrue(Modifier.isStatic(dynamic.getFirst().getModifiers()));
        try (var c = source.getConnection()) { assertEquals(pg.getJdbcUrl(), c.getMetaData().getURL()); }
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success AND script='V1__create_expense_entries.sql'", Integer.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version IS NOT NULL", Integer.class));
        if (getClass().getClassLoader().getResource("jakarta/persistence/Entity.class") != null) {
            assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
        }
    }

    @Test
    void emptyDatabaseDefaultsToOneAndWritesOneParameterizedInfoEvent() throws Exception {
        count("", 0);
        success(1, 0);
    }

    @Test
    void minimumIncludesBoundaryAndOnlyCountsMatchingRows() throws Exception {
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('Below',99),('Boundary',100),('Above',101)");
        count("?minimumAmountMinor=100", 2);
        success(100, 2);
    }

    @Test
    void acceptsLongMaximumAndReturnsZeroIfNothingMatches() throws Exception {
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('Small',1),('Large',?)", Long.MAX_VALUE);
        count("?minimumAmountMinor=" + Long.MAX_VALUE, 1);
        success(Long.MAX_VALUE, 1);
        capture.list.clear();
        sql.update("DELETE FROM expense_entries WHERE amount_minor=?", Long.MAX_VALUE);
        count("?minimumAmountMinor=" + Long.MAX_VALUE, 0);
        success(Long.MAX_VALUE, 0);
    }

    @Test
    void repeatedRequestsUseCurrentDatabaseAndLogEachResultOnce() throws Exception {
        count("", 0); success(1, 0); capture.list.clear();
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('One',10),('Two',20)");
        count("", 2); success(1, 2); capture.list.clear();
        sql.update("DELETE FROM expense_entries WHERE description='One'");
        count("", 1); success(1, 1);
    }

    @Test
    void zeroAndNegativeMinimumReturn400AndOneParameterizedWarnEvent() throws Exception {
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES ('Keep',10)");
        for (long minimum : new long[]{0, -1, Long.MIN_VALUE}) {
            capture.list.clear();
            var response = mvc.perform(get("/bank/expenses/count").param("minimumAmountMinor", Long.toString(minimum))).andReturn().getResponse();
            assertEquals(400, response.getStatus());
            assertEquals("", response.getContentAsString());
            event("event=expense_count_rejected minimumAmountMinor={}", Level.WARN, minimum);
            assertEquals(1L, sql.queryForObject("SELECT count(*) FROM expense_entries", Long.class));
        }
    }

    @Test
    void nonnumericAndOverflowMinimumAre400WithoutSuccessfulBusinessEvent() throws Exception {
        for (String minimum : List.of("abc", "9223372036854775808")) {
            capture.list.clear();
            assertEquals(400, mvc.perform(get("/bank/expenses/count").param("minimumAmountMinor", minimum)).andReturn().getResponse().getStatus());
            assertTrue(businessEvents().stream().noneMatch(e -> e.getFormattedMessage().startsWith("event=expense_count_read ")));
        }
    }

    @Test
    void configuredInfoLevelCanSuppressInfoWhileKeepingWarn() throws Exception {
        assertEquals("INFO", environment.getProperty("logging.level.learning.task098"));
        assertEquals(Level.INFO, packageLogger.getEffectiveLevel());
        packageLogger.setLevel(Level.WARN);
        count("", 0);
        assertTrue(businessEvents().isEmpty(), "INFO должен фильтроваться настройкой уровня логирования.");
        assertEquals(400, mvc.perform(get("/bank/expenses/count").param("minimumAmountMinor", "0")).andReturn().getResponse().getStatus());
        event("event=expense_count_rejected minimumAmountMinor={}", Level.WARN, 0);
    }

    @Test
    void businessLogsDoNotIncludeDescriptionsCredentialsOrAuthorizationHeader() throws Exception {
        String privateDescription = "private-description-do-not-log-98";
        String bearer = "Bearer private-token-do-not-log-98";
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES (?,10)", privateDescription);
        assertEquals(200, mvc.perform(get("/bank/expenses/count").header("Authorization", bearer)).andReturn().getResponse().getStatus());
        success(1, 1);
        for (var log : capture.list) {
            assertFalse(log.getFormattedMessage().contains(privateDescription));
            assertFalse(log.getFormattedMessage().contains(bearer));
            assertFalse(log.getFormattedMessage().contains(environment.getRequiredProperty("spring.datasource.url")));
            assertFalse(log.getFormattedMessage().toLowerCase(java.util.Locale.ROOT).contains("password="));
            assertNull(log.getThrowableProxy());
        }
    }

    @Test
    void migrationKeepsIdentityAndRejectsInvalidDirectSql() {
        assertEquals("YES", sql.queryForObject("SELECT is_identity FROM information_schema.columns WHERE table_schema='public' AND table_name='expense_entries' AND column_name='id'", String.class));
        assertEquals("bigint", sql.queryForObject("SELECT data_type FROM information_schema.columns WHERE table_schema='public' AND table_name='expense_entries' AND column_name='amount_minor'", String.class));
        assertEquals(1, sql.queryForObject("SELECT count(*) FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='expense_entries' AND constraint_type='PRIMARY KEY'", Integer.class));
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
        sql.update("INSERT INTO expense_entries(description,amount_minor) VALUES (?,?)", "a".repeat(100), Long.MAX_VALUE);
        assertEquals(1L, sql.queryForObject("SELECT count(*) FROM expense_entries", Long.class));
    }

    @Test
    void unknownRouteIs404AndPostIs405WithoutSuccessfulBusinessEvent() throws Exception {
        assertEquals(404, mvc.perform(get("/bank/missing")).andReturn().getResponse().getStatus());
        assertEquals(405, mvc.perform(post("/bank/expenses/count")).andReturn().getResponse().getStatus());
        assertTrue(businessEvents().isEmpty());
    }

    private void count(String suffix, long expected) throws Exception {
        var response = mvc.perform(get("/bank/expenses/count" + suffix)).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = json.readTree(response.getContentAsString());
        assertEquals(1, body.size());
        assertTrue(body.path("count").isIntegralNumber());
        assertEquals(expected, body.path("count").longValue());
    }

    private void success(long minimum, long count) {
        event("event=expense_count_read minimumAmountMinor={} count={}", Level.INFO, minimum, count);
    }

    private List<ILoggingEvent> businessEvents() {
        return capture.list.stream().filter(e -> e.getFormattedMessage().startsWith("event=expense_count_")).toList();
    }

    private void event(String template, Level level, long... arguments) {
        var events = businessEvents();
        assertEquals(1, events.size(), "На один обработанный запрос нужно одно бизнес-событие.");
        var event = events.getFirst();
        assertTrue(event.getLoggerName().startsWith("learning.task098."));
        assertEquals(level, event.getLevel());
        assertEquals(template, event.getMessage(), "Передавай значения отдельными аргументами SLF4J, сохраняя {} в шаблоне.");
        assertNull(event.getThrowableProxy());
        Object[] actual = event.getArgumentArray();
        assertNotNull(actual);
        assertEquals(arguments.length, actual.length);
        for (int i = 0; i < arguments.length; i++) {
            assertInstanceOf(Number.class, actual[i]);
            assertEquals(arguments[i], ((Number) actual[i]).longValue());
        }
    }
}
