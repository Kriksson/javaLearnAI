package learning.task091;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.DataInputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/** Nine external-contract checks; no application class or method names are assumed. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComposeBankApiTest {
    private final Path root = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String project = "course091" + UUID.randomUUID().toString().replace("-", "");
    private final Map<String, String> failedStarts = new HashMap<>();
    private ComposeCommands commands;
    private Path context;
    private Path envFile;
    private String builtImage;
    private boolean buildAttempted;

    @BeforeAll
    void packagesAndBuildsOneComposeProject() throws Exception {
        for (String file : List.of("compose.yaml", "Dockerfile", ".dockerignore")) {
            assertTrue(Files.isRegularFile(root.resolve(file)), "Создай " + file + " в корне проекта.");
        }
        commands = new ComposeCommands(root);
        commands.checked(List.of("docker", "compose", "version"), Duration.ofSeconds(10), Map.of());
        commands.checked(List.of("docker", "info"), Duration.ofSeconds(20), Map.of());
        String repository = System.getProperty("maven.repo.local", root.resolve(".maven-home/repository").toString());
        commands.checked(List.of("bash", "./mvnw", "-B", "-Dmaven.repo.local=" + repository,
                "-Dmaven.test.skip=true", "package"), Duration.ofMinutes(4), Map.of());
        assertTrue(Files.isRegularFile(root.resolve("target/bank-service.jar")), "Собери target/bank-service.jar через repackage.");
        context = Files.createTempDirectory("task091-context-");
        Files.createDirectories(context.resolve("target"));
        for (String file : List.of("compose.yaml", "Dockerfile", ".dockerignore", "target/bank-service.jar")) {
            Files.copy(root.resolve(file), context.resolve(file));
        }
        envFile = context.resolve(".env");
        writeEnvironment(null, null);
        assertSingleLocalService(model(Map.of()));
        buildAttempted = true;
        compose(Duration.ofMinutes(5), Map.of(), "build", "api");
        builtImage = commands.checked(List.of("docker", "image", "inspect", "--format", "{{.Id}}", project + "-api"),
                Duration.ofSeconds(10), Map.of()).strip();
    }

    @AfterAll
    void removesOnlyThisTestProject() throws Exception {
        try {
            if (context != null && buildAttempted) {
                compose(Duration.ofSeconds(40), Map.of(), "down", "--remove-orphans", "--rmi", "local", "--timeout", "5");
            }
        } finally {
            ComposeCommands.deleteTree(context);
        }
    }

    @Test
    void executableJarDockerfileAndIgnoreKeepJava21Contract() throws Exception {
        try (JarFile jar = new JarFile(root.resolve("target/bank-service.jar").toFile())) {
            var attrs = jar.getManifest().getMainAttributes();
            assertEquals("org.springframework.boot.loader.launch.JarLauncher", attrs.getValue("Main-Class"));
            String start = attrs.getValue("Start-Class");
            assertNotNull(start);
            assertTrue(start.startsWith("learning.task091."), "Стартовый класс создаётся в learning.task091; имя свободное.");
            var entry = jar.getJarEntry("BOOT-INF/classes/" + start.replace('.', '/') + ".class");
            assertNotNull(entry);
            try (var in = new DataInputStream(jar.getInputStream(entry))) {
                assertEquals(0xCAFEBABE, in.readInt()); in.readUnsignedShort();
                assertEquals(65, in.readUnsignedShort(), "Версия исходников — Java 21.");
            }
            assertTrue(jar.stream().anyMatch(e -> e.getName().startsWith("BOOT-INF/lib/") && e.getName().endsWith(".jar")));
        }
        assertEquals(1, Files.readAllLines(root.resolve("Dockerfile")).stream()
                .filter(s -> s.stripLeading().toUpperCase(Locale.ROOT).startsWith("FROM ")).count());
        Set<String> ignored = new HashSet<>();
        for (String line : Files.readAllLines(root.resolve(".dockerignore"))) {
            String value = line.strip();
            if (value.endsWith("/")) value = value.substring(0, value.length() - 1);
            ignored.add(value);
        }
        for (String value : List.of(".git", ".idea", ".maven-home", "src", "tasks", ".env")) {
            assertTrue(ignored.contains(value), "Добавь " + value + " в .dockerignore.");
        }
        assertTrue(Files.readAllLines(root.resolve(".gitignore")).stream()
                .map(String::strip).anyMatch(s -> s.equals(".env") || s.equals("/.env")),
                "Локальный .env должен быть исключён через .gitignore.");
    }

    @Test
    void composeModelHasLocalBuildDefaultRegionAndLoopbackPort() throws Exception {
        writeEnvironment(null, null);
        JsonNode api = assertSingleLocalService(model(Map.of()));
        assertEquals("Local", api.path("environment").path("BANK_REGION").asText());
        assertPort(api, "8085");
    }

    @Test
    void envFileChangesRegionPortAndPreservesEmptyAndUnicodeValues() throws Exception {
        for (String region : List.of("North", "", "  Восток \"офис\" — №3  ")) {
            writeEnvironment(region, "31391");
            JsonNode api = assertSingleLocalService(model(Map.of()));
            assertEquals(region, api.path("environment").path("BANK_REGION").asText());
            assertPort(api, "31391");
        }
    }

    @Test
    void shellVariablesOverrideEnvFileValues() throws Exception {
        writeEnvironment("FromFile", "31391");
        JsonNode api = assertSingleLocalService(model(Map.of("BANK_REGION", "FromShell", "BANK_HTTP_PORT", "31392")));
        assertEquals("FromShell", api.path("environment").path("BANK_REGION").asText());
        assertPort(api, "31392");
    }

    @Test
    void defaultApiRunsWithoutAuthenticationDatabaseOrHostFiles() throws Exception {
        String url = start(null);
        info(url, "Local"); info(url, "Local");
        String id = containerId();
        JsonNode container = json.readTree(commands.checked(List.of("docker", "inspect", id), Duration.ofSeconds(10), Map.of())).get(0);
        assertEquals(0, container.path("Mounts").size(), "Файлы приложения не монтируются с хоста.");
        var entrypoint = container.path("Config").path("Entrypoint");
        assertTrue(entrypoint.isArray() && entrypoint.size() >= 3, "Используй ENTRYPOINT в JSON-форме.");
        assertTrue(entrypoint.get(0).asText().equals("java") || entrypoint.get(0).asText().endsWith("/java"));
        assertEquals("-jar", entrypoint.get(1).asText());
        String version = commands.checked(List.of("docker", "exec", id, "java", "-version"), Duration.ofSeconds(10), Map.of());
        assertTrue(version.contains("version \"21"));
    }

    @Test
    void regionChangesByRecreatingContainerWithoutRebuildingImage() throws Exception {
        info(start("North"), "North");
        String oldId = containerId();
        info(start("South"), "South");
        assertNotEquals(oldId, containerId());
        assertEquals(builtImage, imageOfContainer(), "Смена окружения не требует нового образа.");
    }

    @Test
    void actualHttpPreservesUnicodeSpacesQuotesAndEmptyRegion() throws Exception {
        String region = "  Восток \"офис\" — №3  ";
        info(start(region), region);
        info(start(""), "");
    }

    @Test
    void unmappedPathIs404AndPostToReadEndpointIs405() throws Exception {
        String url = start("Errors");
        assertEquals(404, send(url + "/missing", "GET").statusCode());
        assertEquals(405, send(url + "/bank/status", "POST").statusCode());
        info(url, "Errors");
    }

    @Test
    void downRemovesContainerAndNextUpReusesSavedImage() throws Exception {
        info(start("Before"), "Before");
        String oldId = containerId();
        compose(Duration.ofSeconds(30), Map.of(), "down", "--timeout", "5");
        assertNotEquals(0, commands.run(List.of("docker", "inspect", oldId), Duration.ofSeconds(10), Map.of()).code());
        String remaining = commands.checked(List.of("docker", "ps", "-aq", "--filter", "label=com.docker.compose.project=" + project),
                Duration.ofSeconds(10), Map.of()).strip();
        assertEquals("", remaining);
        info(start("After"), "After");
        assertEquals(builtImage, imageOfContainer());
    }

    private JsonNode assertSingleLocalService(JsonNode model) {
        JsonNode services = model.path("services");
        assertEquals(1, services.size(), "В первой Compose-практике ровно один сервис api.");
        assertTrue(services.has("api"));
        JsonNode api = services.get("api");
        assertFalse(api.has("image"), "Оставь имя образа Compose: в этой практике используется build без image.");
        assertFalse(api.has("container_name"), "Не закрепляй container_name: Compose должен изолировать проекты.");
        assertEquals(0, api.path("volumes").size(), "Монтирование файлов с хоста не требуется.");
        assertEquals(0, api.path("env_file").size(), "Используй подстановку в environment, а не env_file сервиса.");
        assertEquals(context.toString(), api.path("build").path("context").asText(), "Контекст build — текущий каталог проекта.");
        assertEquals("Dockerfile", api.path("build").path("dockerfile").asText());
        assertEquals(1, api.path("ports").size());
        return api;
    }

    private void assertPort(JsonNode api, String published) {
        JsonNode port = api.path("ports").get(0);
        assertEquals("127.0.0.1", port.path("host_ip").asText());
        assertEquals(8080, port.path("target").asInt());
        assertEquals(published, port.path("published").asText());
        assertEquals("tcp", port.path("protocol").asText());
    }

    private JsonNode model(Map<String, String> environment) throws Exception {
        return json.readTree(compose(Duration.ofSeconds(15), environment, "config", "--format", "json"));
    }

    private String compose(Duration timeout, Map<String, String> environment, String... arguments) throws Exception {
        List<String> cmd = new ArrayList<>(List.of("docker", "compose", "-f", context.resolve("compose.yaml").toString(),
                "--env-file", envFile.toString(), "-p", project));
        cmd.addAll(List.of(arguments));
        return commands.checked(cmd, timeout, environment);
    }

    private void writeEnvironment(String region, String port) throws Exception {
        String contents = port == null ? "" : "BANK_HTTP_PORT=" + port + "\n";
        if (region != null) contents += "BANK_REGION='" + region + "'\n";
        Files.writeString(envFile, contents);
    }

    private String start(String region) throws Exception {
        if (failedStarts.containsKey(region)) throw new AssertionError(failedStarts.get(region));
        writeEnvironment(region, "0");
        compose(Duration.ofSeconds(40), Map.of(), "up", "-d", "--no-build", "--force-recreate", "api");
        String id = containerId();
        String mapped = compose(Duration.ofSeconds(10), Map.of(), "port", "api", "8080").strip();
        String address = Arrays.stream(mapped.split("\\R")).filter(s -> s.startsWith("127.0.0.1:")).findFirst().orElseThrow();
        String url = "http://127.0.0.1:" + Integer.parseInt(address.substring(address.lastIndexOf(':') + 1));
        long until = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        long nextCheck = System.nanoTime();
        while (System.nanoTime() < until) {
            try {
                if (send(url + "/bank/status", "GET").statusCode() == 200) return url;
            } catch (java.io.IOException notReady) { /* Server is still starting. */ }
            if (System.nanoTime() >= nextCheck) {
                String state = commands.checked(List.of("docker", "inspect", "--format", "{{.State.Status}}", id),
                        Duration.ofSeconds(10), Map.of()).strip();
                if (state.equals("exited") || state.equals("dead")) throw failed(region, "Контейнер завершился до готовности API.");
                nextCheck = System.nanoTime() + Duration.ofSeconds(1).toNanos();
            }
            Thread.sleep(200);
        }
        throw failed(region, "API не выдало 200 за 90 секунд.");
    }

    private AssertionError failed(String region, String reason) throws Exception {
        String message = reason + "\n" + compose(Duration.ofSeconds(10), Map.of(), "logs", "--no-color", "api");
        failedStarts.put(region, message);
        return new AssertionError(message);
    }

    private String containerId() throws Exception {
        String id = compose(Duration.ofSeconds(10), Map.of(), "ps", "-a", "-q", "api").strip();
        assertFalse(id.isEmpty(), "Compose должен создать контейнер сервиса api.");
        assertFalse(id.contains("\n"));
        return id;
    }

    private String imageOfContainer() throws Exception {
        return commands.checked(List.of("docker", "inspect", "--format", "{{.Image}}", containerId()),
                Duration.ofSeconds(10), Map.of()).strip();
    }

    private HttpResponse<String> send(String url, String method) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void info(String url, String region) throws Exception {
        var response = send(url + "/bank/status", "GET");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(json.valueToTree(Map.of("service", "Transfer API", "region", region)), json.readTree(response.body()),
                "JSON содержит ровно service и region с неизменённым значением настройки.");
    }
}
