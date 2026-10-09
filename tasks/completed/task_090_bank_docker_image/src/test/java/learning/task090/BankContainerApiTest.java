package learning.task090;

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

/** Real container tests. No Spring application classes or internal names are referenced. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BankContainerApiTest {
    private final Path root = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final List<String> containers = new ArrayList<>();
    private final Map<String, String> urls = new HashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> startupFailures = new HashMap<>();
    private final String image = "java-course-task090:" + UUID.randomUUID().toString().replace("-", "");
    private ContainerCommands commands;
    private String engine;
    private boolean imageAttempted;
    private Path context;

    @BeforeAll
    void buildsExecutableJarAndImageInSeparateContext() throws Exception {
        assertTrue(Files.isRegularFile(root.resolve("Dockerfile")), "Создай Dockerfile в корне проекта.");
        assertTrue(Files.isRegularFile(root.resolve(".dockerignore")), "Создай .dockerignore в корне проекта.");
        commands = new ContainerCommands(root);
        // This nested build packages the application only; it never runs these tests recursively.
        String repository = System.getProperty("maven.repo.local", root.resolve(".maven-home/repository").toString());
        commands.checked(List.of("bash", "./mvnw", "-B", "-Dmaven.repo.local=" + repository,
                "-Dmaven.test.skip=true", "package"), Duration.ofMinutes(4));
        assertTrue(Files.isRegularFile(root.resolve("target/bank-service.jar")),
                "Собери исполняемый Spring Boot JAR target/bank-service.jar через Maven package.");
        engine = chooseEngine();
        commands.checked(List.of(engine, "info"), Duration.ofSeconds(20));
        context = Files.createTempDirectory("task090-build-context-");
        Files.createDirectories(context.resolve("target"));
        Files.copy(root.resolve("target/bank-service.jar"), context.resolve("target/bank-service.jar"));
        Files.copy(root.resolve("Dockerfile"), context.resolve("Dockerfile"));
        Files.copy(root.resolve(".dockerignore"), context.resolve(".dockerignore"));
        imageAttempted = true;
        commands.checked(List.of(engine, "build", "--no-cache", "-t", image, context.toString()), Duration.ofMinutes(5));
        // The image must work after the build context has gone away, without mounting host files.
        ContainerCommands.deleteTree(context); context = null;
    }

    @AfterAll
    void removesOnlyOwnContainersAndImage() throws Exception {
        List<String> errors = new ArrayList<>();
        if (engine != null && commands != null) {
            for (String name : containers) {
                if (commands.run(List.of(engine, "inspect", name), Duration.ofSeconds(20)).code() != 0) continue;
                var result = commands.run(List.of(engine, "rm", "-f", name), Duration.ofSeconds(30));
                if (result.code() != 0) errors.add("Не удалось очистить тестовый контейнер " + name + ": " + result.output());
            }
            if (imageAttempted) {
                // A failed build may not have created an image at all.
                var exists = commands.run(List.of(engine, "image", "inspect", image), Duration.ofSeconds(20));
                if (exists.code() == 0) {
                    var removed = commands.run(List.of(engine, "image", "rm", image), Duration.ofSeconds(30));
                    if (removed.code() != 0) errors.add("Не удалось очистить тестовый образ: " + removed.output());
                }
            }
        }
        if (context != null) ContainerCommands.deleteTree(context);
        assertEquals(List.of(), errors);
    }

    @Test
    void jarContainsBootLauncherDependenciesAndJava21Application() throws Exception {
        try (JarFile jar = new JarFile(root.resolve("target/bank-service.jar").toFile())) {
            assertNotNull(jar.getManifest());
            var attributes = jar.getManifest().getMainAttributes();
            assertEquals("org.springframework.boot.loader.launch.JarLauncher", attributes.getValue("Main-Class"),
                    "Нужен исполняемый Spring Boot JAR: подключи выполнение repackage, обычного jar недостаточно.");
            String entry = attributes.getValue("Start-Class");
            assertNotNull(entry); assertTrue(entry.startsWith("learning.task090."), "Стартовый класс должен быть в learning.task090; его имя свободное.");
            var app = jar.getJarEntry("BOOT-INF/classes/" + entry.replace('.', '/') + ".class");
            assertNotNull(app);
            try (var input = new DataInputStream(jar.getInputStream(app))) {
                assertEquals(0xCAFEBABE, input.readInt()); input.readUnsignedShort();
                assertEquals(65, input.readUnsignedShort(), "Исходники должны компилироваться для Java 21.");
            }
            assertTrue(jar.stream().anyMatch(e -> e.getName().startsWith("BOOT-INF/lib/") && e.getName().endsWith(".jar")),
                    "Зависимости приложения должны быть внутри исполняемого архива.");
        }
    }

    @Test
    void dockerfileUsesSingleStageAndIgnoresNonApplicationDirectories() throws Exception {
        long stages = Files.readAllLines(root.resolve("Dockerfile")).stream()
                .filter(line -> line.stripLeading().toUpperCase(Locale.ROOT).startsWith("FROM ")).count();
        assertEquals(1, stages, "В первой практике один этап Dockerfile; JAR собирается на хосте.");
        Set<String> ignored = new HashSet<>();
        for (String line : Files.readAllLines(root.resolve(".dockerignore"))) {
            String value = line.strip();
            if (value.endsWith("/")) value = value.substring(0, value.length()-1);
            ignored.add(value);
        }
        for (String entry : List.of(".git", ".idea", ".maven-home", "src", "tasks", ".env")) {
            assertTrue(ignored.contains(entry), "Добавь отдельную строку " + entry + " в .dockerignore; завершающий / допустим.");
        }
    }

    @Test
    void defaultBranchWorksWithoutAuthenticationDatabaseOrHostMounts() throws Exception {
        String url = running(null);
        info(url, "Central"); info(url, "Central");
        String name = names.get(null);
        JsonNode inspect = json.readTree(commands.checked(List.of(engine, "inspect", name), Duration.ofSeconds(20))).get(0);
        assertTrue(inspect.path("State").path("Running").asBoolean());
        assertEquals(0, inspect.path("Mounts").size(), "Контейнер должен работать из образа, без файлов приложения с хоста.");
        var entrypoint = inspect.path("Config").path("Entrypoint");
        assertTrue(entrypoint.isArray() && entrypoint.size() >= 3,
                "Задай ENTRYPOINT в JSON-форме: Java, -jar, путь к JAR внутри образа.");
        String executable = entrypoint.get(0).asText();
        assertTrue(executable.equals("java") || executable.endsWith("/java"));
        assertEquals("-jar", entrypoint.get(1).asText());
        String version = commands.checked(List.of(engine, "exec", name, "java", "-version"), Duration.ofSeconds(20));
        assertTrue(version.contains("version \"21"), "В образе требуется Java runtime 21: " + version);
    }

    @Test
    void environmentChangesBranchWithoutRebuildingImage() throws Exception {
        info(running("Retail"), "Retail");
        info(running(null), "Central");
    }

    @Test
    void branchPreservesUnicodeQuotesSpacesAndEmptyEnvironmentValue() throws Exception {
        String branch = "  Северный \"офис\" — №2  ";
        info(running(branch), branch);
        info(running(""), "");
    }

    @Test
    void unmappedPathIs404AndPostToReadEndpointIs405() throws Exception {
        String url = running(null);
        assertEquals(404, send(url + "/missing", "GET").statusCode());
        assertEquals(405, send(url + "/bank/info", "POST").statusCode());
        info(url, "Central");
    }

    private String chooseEngine() throws Exception {
        try {
            commands.checked(List.of("docker", "--version"), Duration.ofSeconds(10));
        } catch (java.io.IOException missingExecutable) {
            throw new IllegalStateException("Задача №90 требует Docker Engine и команды docker. Установи Docker CLI и запусти daemon.", missingExecutable);
        }
        return "docker";
    }

    private String running(String branch) throws Exception {
        if (startupFailures.containsKey(branch)) throw new AssertionError(startupFailures.get(branch));
        if (urls.containsKey(branch)) return urls.get(branch);
        String name = "course-task090-" + UUID.randomUUID().toString().replace("-", "");
        List<String> args = new ArrayList<>(List.of(engine, "run", "-d", "--name", name,
                "-p", "127.0.0.1::8080"));
        if (branch != null) args.addAll(List.of("-e", "BANK_BRANCH=" + branch));
        args.add(image);
        containers.add(name);
        commands.checked(args, Duration.ofSeconds(30));
        String mapped = commands.checked(List.of(engine, "port", name, "8080/tcp"), Duration.ofSeconds(20)).strip();
        String address = Arrays.stream(mapped.split("\\R")).filter(line -> line.startsWith("127.0.0.1:")).findFirst().orElseThrow();
        String url = "http://127.0.0.1:" + Integer.parseInt(address.substring(address.lastIndexOf(':')+1));
        long until = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        long nextStateCheck = System.nanoTime();
        while (System.nanoTime() < until) {
            try {
                if (send(url + "/bank/info", "GET").statusCode() == 200) {
                    urls.put(branch, url); names.put(branch, name); return url;
                }
            } catch (java.io.IOException notReady) { /* Wait for this container's server. */ }
            if (System.nanoTime() >= nextStateCheck) {
                String state = commands.checked(List.of(engine, "inspect", "--format", "{{.State.Status}}", name),
                        Duration.ofSeconds(10)).strip();
                if (state.equals("exited") || state.equals("dead")) {
                    throw startupFailure(branch, name,
                            "Контейнер завершился до готовности API (состояние " + state + "). Проверь причину в логах ниже.");
                }
                nextStateCheck = System.nanoTime() + Duration.ofSeconds(1).toNanos();
            }
            Thread.sleep(200);
        }
        throw startupFailure(branch, name,
                "Контейнер не выдал 200 на /bank/info за 90 секунд. Проверь запуск JAR, порт 8080 и конфигурацию.");
    }

    private AssertionError startupFailure(String branch, String name, String reason) throws Exception {
        String logs = commands.run(List.of(engine, "logs", name), Duration.ofSeconds(10)).output();
        String message = reason + "\n" + logs;
        startupFailures.put(branch, message);
        return new AssertionError(message);
    }

    private HttpResponse<String> send(String url, String method) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void info(String url, String branch) throws Exception {
        var response = send(url + "/bank/info", "GET");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(json.valueToTree(Map.of("name", "Practice Bank", "branch", branch)), json.readTree(response.body()),
                "Ответ содержит ровно name и branch, со значением из окружения или Central по умолчанию.");
    }
}
