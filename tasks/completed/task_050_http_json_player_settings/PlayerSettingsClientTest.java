package learning.task050;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSettingsClientTest {
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("{\"playerId\":7,\"notificationsEnabled\":true,\"theme\":\"тёмная\"}");
    private final AtomicReference<String> responseContentType = new AtomicReference<>("application/json; charset=UTF-8");
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> accept = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            path.set(exchange.getRequestURI().getPath());
            method.set(exchange.getRequestMethod());
            accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            exchange.getResponseHeaders().set("Content-Type", responseContentType.get());
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/");
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getsTypedSettingsAndSendsCorrectGet() throws Exception {
        assertEquals(Optional.of(new PlayerSettings(7, true, "тёмная")),
                new PlayerSettingsClient(baseUri).find(7));
        assertEquals(1, requests.get());
        assertEquals("/api/players/7/settings", path.get());
        assertEquals("GET", method.get());
        assertEquals("application/json", accept.get());
    }

    @Test
    void acceptsFalseAndKeepsWhitespaceInThemeAndIgnoresExtraFields() throws Exception {
        body.set("{\"theme\":\" светлая \",\"extra\":1,\"notificationsEnabled\":false,\"playerId\":1}");
        assertEquals(Optional.of(new PlayerSettings(1, false, " светлая ")),
                new PlayerSettingsClient(baseUri).find(1));
    }

    @Test
    void acceptsUpperIntegerBoundary() throws Exception {
        body.set("{\"playerId\":2147483647,\"notificationsEnabled\":false,\"theme\":\"dark\"}");
        assertEquals(Optional.of(new PlayerSettings(Integer.MAX_VALUE, false, "dark")),
                new PlayerSettingsClient(baseUri).find(Integer.MAX_VALUE));
    }

    @Test
    void readsUtf8EvenIfResponseHeaderSpecifiesAnotherCharset() throws Exception {
        responseContentType.set("application/json; charset=ISO-8859-1");
        assertEquals(Optional.of(new PlayerSettings(7, true, "тёмная")),
                new PlayerSettingsClient(baseUri).find(7));
    }

    @Test
    void returnsEmptyOn404BeforeParsingTextBody() throws Exception {
        status.set(404);
        body.set("Not found");
        assertEquals(Optional.empty(), new PlayerSettingsClient(baseUri).find(999));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 503})
    void reportsOtherStatusBeforeParsingTextBody(int code) {
        status.set(code);
        body.set("Unavailable");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerSettingsClient(baseUri).find(7));
        assertTrue(error.getMessage().contains(String.valueOf(code)));
    }

    @Test
    void rejectsInvalidIdsBeforeRequest() {
        PlayerSettingsClient client = new PlayerSettingsClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.find(0));
        assertThrows(IllegalArgumentException.class, () -> client.find(-1));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUris() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerSettingsClient(null));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSettingsClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSettingsClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSettingsClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerSettingsClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "42", "{}",
            "{\"playerId\":7,\"notificationsEnabled\":true}",
            "{\"playerId\":7,\"theme\":\"dark\"}",
            "{\"notificationsEnabled\":true,\"theme\":\"dark\"}",
            "{\"playerId\":0,\"notificationsEnabled\":true,\"theme\":\"dark\"}",
            "{\"playerId\":8,\"notificationsEnabled\":true,\"theme\":\"dark\"}",
            "{\"playerId\":2147483648,\"notificationsEnabled\":true,\"theme\":\"dark\"}",
            "{\"playerId\":1.5,\"notificationsEnabled\":true,\"theme\":\"dark\"}",
            "{\"playerId\":\"7\",\"notificationsEnabled\":true,\"theme\":\"dark\"}",
            "{\"playerId\":7,\"notificationsEnabled\":null,\"theme\":\"dark\"}",
            "{\"playerId\":7,\"notificationsEnabled\":\"true\",\"theme\":\"dark\"}",
            "{\"playerId\":7,\"notificationsEnabled\":1,\"theme\":\"dark\"}",
            "{\"playerId\":7,\"notificationsEnabled\":true,\"theme\":null}",
            "{\"playerId\":7,\"notificationsEnabled\":true,\"theme\":\"\"}",
            "{\"playerId\":7,\"notificationsEnabled\":true,\"theme\":\"\u2003\"}",
            "{\"playerId\":7,\"notificationsEnabled\":true,\"theme\":12}"
    })
    void rejectsInvalid200Responses(String invalidBody) {
        body.set(invalidBody);
        assertThrows(IllegalStateException.class, () -> new PlayerSettingsClient(baseUri).find(7));
    }

    @Test
    void keepsMalformedJsonCause() {
        body.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerSettingsClient(baseUri).find(7));
        assertInstanceOf(JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class, () -> new PlayerSettingsClient(baseUri).find(7));
    }
}
