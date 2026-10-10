package learning.task092;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.DataInputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresComposeApiTest {
    private final Path root = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String project = "course092" + UUID.randomUUID().toString().replace("-", "");
    private final String database = "test_bank_" + UUID.randomUUID().toString().substring(0, 8);
    private final String user = "app_test";
    private final String password = UUID.randomUUID().toString();
    private ComposeCommands commands;
    private Path context;
    private Path runtimeEnv;
    private String url;
    private boolean attempted;

    @BeforeAll
    void buildsApiAndStartsPostgresBeforeApi() throws Exception {
        for (String file : List.of("compose.yaml", "Dockerfile", ".dockerignore")) {
            assertTrue(Files.isRegularFile(root.resolve(file)), "Создай " + file + " для задачи №92.");
        }
        commands = new ComposeCommands(root);
        commands.checked(List.of("docker", "compose", "version"), Duration.ofSeconds(10), Map.of());
        commands.checked(List.of("docker", "info"), Duration.ofSeconds(20), Map.of());
        String repository = System.getProperty("maven.repo.local", root.resolve(".maven-home/repository").toString());
        commands.checked(List.of("bash", "./mvnw", "-B", "-Dmaven.repo.local=" + repository,
                "-Dmaven.test.skip=true", "package"), Duration.ofMinutes(4), Map.of());
        assertTrue(Files.isRegularFile(root.resolve("target/bank-service.jar")));
        context = Files.createTempDirectory("task092-context-");
        Files.createDirectories(context.resolve("target"));
        for (String file : List.of("compose.yaml", "Dockerfile", ".dockerignore", "target/bank-service.jar")) {
            Files.copy(root.resolve(file), context.resolve(file));
        }
        runtimeEnv = context.resolve(".env");
        Files.writeString(runtimeEnv, "BANK_HTTP_PORT=0\nBANK_DB_NAME=" + database + "\nBANK_DB_USER=" + user
                + "\nBANK_DB_PASSWORD=" + password + "\n");
        assertModel(model(runtimeEnv), database, user, password, "0");
        attempted = true;
        compose(runtimeEnv, Duration.ofMinutes(5), "build", "api");
        compose(runtimeEnv, Duration.ofMinutes(5), "up", "-d", "db");
        waitForDatabase();
        startApi();
    }

    @AfterAll
    void removesOnlyOwnProjectIncludingItsDatabaseData() throws Exception {
        try {
            if (attempted) compose(runtimeEnv, Duration.ofSeconds(45), "down", "--volumes", "--rmi", "local", "--timeout", "5");
        } finally {
            ComposeCommands.deleteTree(context);
        }
    }

    @BeforeEach
    void emptyPaymentRequests() throws Exception {
        sql("TRUNCATE payment_requests RESTART IDENTITY");
    }

    @Test
    void composeDefaultsAndFileOverridesConfigureBothServices() throws Exception {
        Path defaults = context.resolve("defaults.env"); Files.writeString(defaults, "");
        assertModel(model(defaults), "bank_lab", "bank_app", "learning-only", "8085");
        Path custom = context.resolve("custom.env");
        Files.writeString(custom, "BANK_DB_NAME=finance_demo\nBANK_DB_USER=report_user\nBANK_DB_PASSWORD=sample-value\nBANK_HTTP_PORT=31393\n");
        assertModel(model(custom), "finance_demo", "report_user", "sample-value", "31393");
    }

    @Test
    void java21ExecutableJarContainsMigrationAndDockerIgnoresLocalFiles() throws Exception {
        try (JarFile jar = new JarFile(root.resolve("target/bank-service.jar").toFile())) {
            var attrs = jar.getManifest().getMainAttributes();
            assertEquals("org.springframework.boot.loader.launch.JarLauncher", attrs.getValue("Main-Class"));
            String main = attrs.getValue("Start-Class"); assertNotNull(main); assertTrue(main.startsWith("learning.task092."));
            var entry = jar.getJarEntry("BOOT-INF/classes/" + main.replace('.', '/') + ".class"); assertNotNull(entry);
            try (var in = new DataInputStream(jar.getInputStream(entry))) {
                assertEquals(0xCAFEBABE, in.readInt()); in.readUnsignedShort(); assertEquals(65, in.readUnsignedShort());
            }
            assertNotNull(jar.getJarEntry("BOOT-INF/classes/db/migration/V1__create_payment_requests.sql"));
        }
        assertEquals(1, Files.readAllLines(root.resolve("Dockerfile")).stream()
                .filter(s -> s.stripLeading().toUpperCase(Locale.ROOT).startsWith("FROM ")).count());
        Set<String> ignored = new HashSet<>();
        for (String line : Files.readAllLines(root.resolve(".dockerignore"))) {
            String value = line.strip(); if (value.endsWith("/")) value = value.substring(0, value.length() - 1); ignored.add(value);
        }
        for (String name : List.of(".git", ".idea", ".maven-home", "src", "tasks", ".env")) assertTrue(ignored.contains(name));
        assertTrue(Files.readAllLines(root.resolve(".gitignore")).stream().map(String::strip)
                .anyMatch(s -> s.equals(".env") || s.equals("/.env")));
    }

    @Test
    void emptyDatabaseReturnsZeroAndHasSuccessfulFlywayV1() throws Exception {
        count(0);
        assertEquals("1", sql("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success AND script='V1__create_payment_requests.sql'").strip());
        assertEquals("1", sql("SELECT count(*) FROM flyway_schema_history WHERE version IS NOT NULL").strip());
    }

    @Test
    void countReadsCurrentInsertsAndDeletesFromPostgres() throws Exception {
        sql("INSERT INTO payment_requests(description) VALUES ('Rent'), ('Travel')"); count(2);
        sql("DELETE FROM payment_requests WHERE description='Rent'"); count(1);
        sql("DELETE FROM payment_requests"); count(0);
    }

    @Test
    void manyRowsAreCountedAsAnIntegerWithoutReturningRecords() throws Exception {
        sql("INSERT INTO payment_requests(description) SELECT 'Payment ' || n FROM generate_series(1,125) n"); count(125);
    }

    @Test
    void migrationKeepsIdentityAndDescriptionConstraints() throws Exception {
        assertEquals("YES|bigint", sql("SELECT is_identity || '|' || data_type FROM information_schema.columns WHERE table_schema='public' AND table_name='payment_requests' AND column_name='id'").strip());
        assertEquals("1", sql("SELECT count(*) FROM information_schema.table_constraints WHERE table_schema='public' AND table_name='payment_requests' AND constraint_type='PRIMARY KEY'").strip());
        sql("INSERT INTO payment_requests(description) VALUES ('" + "a".repeat(100) + "')");
        assertEquals("1", sql("SELECT count(*) FROM payment_requests WHERE id IS NOT NULL AND id>0").strip());
        assertNotEquals(0, sqlResult("INSERT INTO payment_requests(description) VALUES (NULL)").code());
        assertNotEquals(0, sqlResult("INSERT INTO payment_requests(description) VALUES ('" + "a".repeat(101) + "')").code());
        count(1);
    }

    @Test
    void apiRecreationReusesRunningDatabaseAndExistingMigration() throws Exception {
        sql("INSERT INTO payment_requests(description) VALUES ('Invoice'), ('Subscription')");
        String db = id("db"); String image = inspect(id("api")).path("Image").asText();
        startApi(); count(2);
        assertEquals(db, id("db"), "Пересоздаём только API; PostgreSQL продолжает работать.");
        assertEquals(image, inspect(id("api")).path("Image").asText());
        assertEquals("1", sql("SELECT count(*) FROM flyway_schema_history WHERE version='1' AND success").strip());
    }

    @Test
    void containersShareDefaultNetworkAndDatabaseHasNoPublishedPort() throws Exception {
        JsonNode api = inspect(id("api")); JsonNode db = inspect(id("db"));
        assertTrue(api.path("NetworkSettings").path("Networks").has(project + "_default"));
        assertTrue(db.path("NetworkSettings").path("Networks").has(project + "_default"));
        assertEquals(0, db.path("HostConfig").path("PortBindings").size());
        assertEquals(0, api.path("Mounts").size());
        var entrypoint = api.path("Config").path("Entrypoint");
        assertTrue(entrypoint.isArray() && entrypoint.size() >= 3);
        assertTrue(entrypoint.get(0).asText().equals("java") || entrypoint.get(0).asText().endsWith("/java"));
        assertEquals("-jar", entrypoint.get(1).asText());
        assertTrue(commands.checked(List.of("docker", "exec", id("api"), "java", "-version"), Duration.ofSeconds(10), Map.of())
                .contains("version \"21"));
        assertEquals(database, sql("SELECT current_database()").strip());
        assertTrue(sql("SELECT version()").startsWith("PostgreSQL 17"));
    }

    @Test
    void missingPathIs404AndPostToCountIs405() throws Exception {
        assertEquals(404, send("/missing", "GET").statusCode());
        assertEquals(405, send("/bank/payments/count", "POST").statusCode());
        count(0);
    }

    private void assertModel(JsonNode model, String name, String login, String secret, String port) {
        JsonNode services = model.path("services"); assertEquals(2, services.size()); assertTrue(services.has("api") && services.has("db"));
        JsonNode api = services.get("api"), db = services.get("db");
        assertEquals(context.toString(), api.path("build").path("context").asText());
        assertEquals("Dockerfile", api.path("build").path("dockerfile").asText());
        assertFalse(api.has("image"));
        String image = db.path("image").asText(); assertTrue(image.equals("postgres:17-alpine") || image.equals("docker.io/library/postgres:17-alpine"));
        assertFalse(db.has("build"));
        for (JsonNode service : List.of(api, db)) {
            assertFalse(service.has("container_name")); assertFalse(service.has("network_mode"));
            assertEquals(0, service.path("volumes").size()); assertEquals(0, service.path("env_file").size());
            assertFalse(service.has("healthcheck"));
            assertEquals(1, service.path("networks").size()); assertTrue(service.path("networks").has("default"));
        }
        assertEquals(0, db.path("ports").size());
        assertFalse(db.path("environment").has("POSTGRES_HOST_AUTH_METHOD"));
        assertEquals(name, db.path("environment").path("POSTGRES_DB").asText());
        assertEquals(login, db.path("environment").path("POSTGRES_USER").asText());
        assertEquals(secret, db.path("environment").path("POSTGRES_PASSWORD").asText());
        assertEquals("jdbc:postgresql://db:5432/" + name, api.path("environment").path("SPRING_DATASOURCE_URL").asText());
        assertEquals(login, api.path("environment").path("SPRING_DATASOURCE_USERNAME").asText());
        assertEquals(secret, api.path("environment").path("SPRING_DATASOURCE_PASSWORD").asText());
        assertEquals(1, api.path("ports").size()); var mapping = api.path("ports").get(0);
        assertEquals("127.0.0.1", mapping.path("host_ip").asText()); assertEquals(8080, mapping.path("target").asInt());
        assertEquals(port, mapping.path("published").asText()); assertEquals("tcp", mapping.path("protocol").asText());
    }

    private JsonNode model(Path env) throws Exception { return json.readTree(compose(env, Duration.ofSeconds(15), "config", "--format", "json")); }
    private String compose(Path env, Duration timeout, String... arguments) throws Exception {
        List<String> cmd = new ArrayList<>(List.of("docker", "compose", "-f", context.resolve("compose.yaml").toString(), "--env-file", env.toString(), "-p", project));
        cmd.addAll(List.of(arguments)); return commands.checked(cmd, timeout, Map.of());
    }
    private String id(String service) throws Exception {
        String value = compose(runtimeEnv, Duration.ofSeconds(10), "ps", "-a", "-q", service).strip(); assertFalse(value.isEmpty()); return value;
    }
    private JsonNode inspect(String id) throws Exception { return json.readTree(commands.checked(List.of("docker", "inspect", id), Duration.ofSeconds(10), Map.of())).get(0); }
    private void waitForDatabase() throws Exception {
        long until = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < until) {
            JsonNode state = inspect(id("db")).path("State");
            if (!state.path("Running").asBoolean()) fail(compose(runtimeEnv, Duration.ofSeconds(10), "logs", "--no-color", "db"));
            // Image initialization starts a temporary Unix-socket-only server. Wait for the final TCP server.
            if (sqlResult("SELECT 1").code() == 0) return;
            Thread.sleep(300);
        }
        fail("PostgreSQL не готов за 60 секунд.\n" + compose(runtimeEnv, Duration.ofSeconds(10), "logs", "--no-color", "db"));
    }
    private ComposeCommands.Result sqlResult(String sql) throws Exception {
        return commands.run(List.of("docker", "exec", "-e", "PGPASSWORD=" + password, id("db"), "psql", "-h", "db", "-U", user,
                "-d", database, "-v", "ON_ERROR_STOP=1", "-A", "-t", "-c", sql), Duration.ofSeconds(10), Map.of());
    }
    private String sql(String sql) throws Exception {
        var r = sqlResult(sql); assertEquals(0, r.code(), r.output()); return r.output();
    }
    private void startApi() throws Exception {
        compose(runtimeEnv, Duration.ofSeconds(40), "up", "-d", "--no-build", "--no-deps", "--force-recreate", "api");
        String mapped = compose(runtimeEnv, Duration.ofSeconds(10), "port", "api", "8080").strip();
        url = "http://127.0.0.1:" + Integer.parseInt(mapped.substring(mapped.lastIndexOf(':') + 1));
        long until = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < until) {
            try { if (send("/bank/payments/count", "GET").statusCode() == 200) return; }
            catch (java.io.IOException starting) { /* Wait for server startup. */ }
            if (!inspect(id("api")).path("State").path("Running").asBoolean()) {
                fail("API завершилось при запуске.\n" + compose(runtimeEnv, Duration.ofSeconds(10), "logs", "--no-color", "api"));
            }
            Thread.sleep(300);
        }
        fail("API не выдало 200 за 90 секунд.\n" + compose(runtimeEnv, Duration.ofSeconds(10), "logs", "--no-color", "api"));
    }
    private HttpResponse<String> send(String path, String method) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url + path)).timeout(Duration.ofSeconds(3))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }
    private void count(long expected) throws Exception {
        var response = send("/bank/payments/count", "GET"); assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        JsonNode body = json.readTree(response.body()); assertEquals(1, body.size()); assertTrue(body.path("count").isIntegralNumber());
        assertEquals(expected, body.path("count").asLong());
    }
}
