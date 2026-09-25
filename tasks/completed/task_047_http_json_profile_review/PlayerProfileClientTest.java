package learning.task047;

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

class PlayerProfileClientTest {
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("{\"id\":7,\"name\":\"Лис\",\"level\":12}");
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
            byte[] response = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
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
    void returnsTypedProfileAndSendsCorrectGet() throws Exception {
        assertEquals(Optional.of(new PlayerProfile(7, "Лис", 12)), new PlayerProfileClient(baseUri).find(7));
        assertEquals(1, requests.get());
        assertEquals("/api/players/7", path.get());
        assertEquals("GET", method.get());
        assertEquals("application/json", accept.get());
    }

    @Test
    void ignoresJsonFieldOrderAndUnknownFieldsAndKeepsName() throws Exception {
        body.set(" { \"level\": 1, \"extra\": true, \"name\": \" Лис \", \"id\": 1 } ");
        assertEquals(Optional.of(new PlayerProfile(1, " Лис ", 1)), new PlayerProfileClient(baseUri).find(1));
    }

    @Test
    void acceptsUpperBoundaries() throws Exception {
        body.set("{\"id\":2147483647,\"name\":\"Герой\",\"level\":100}");
        assertEquals(Optional.of(new PlayerProfile(Integer.MAX_VALUE, "Герой", 100)),
                new PlayerProfileClient(baseUri).find(7));
    }

    @Test
    void returnsEmptyFor404EvenWithNonJsonBody() throws Exception {
        status.set(404);
        body.set("Not found");
        assertEquals(Optional.empty(), new PlayerProfileClient(baseUri).find(999));
        assertEquals("/api/players/999", path.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 503})
    void reportsOtherStatusBeforeParsingBody(int code) {
        status.set(code);
        body.set("Service unavailable");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerProfileClient(baseUri).find(7));
        assertTrue(error.getMessage().contains(String.valueOf(code)));
    }

    @Test
    void rejectsInvalidIdsBeforeRequest() {
        PlayerProfileClient client = new PlayerProfileClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.find(0));
        assertThrows(IllegalArgumentException.class, () -> client.find(-1));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUris() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfileClient(null));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfileClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfileClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfileClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfileClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "42", "{}", "{\"id\":7,\"name\":\"Лис\"}",
            "{\"id\":7,\"level\":12}", "{\"name\":\"Лис\",\"level\":12}",
            "{\"id\":0,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":-1,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":2147483648,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":1.5,\"name\":\"Лис\",\"level\":12}",
            "{\"id\":\"7\",\"name\":\"Лис\",\"level\":12}",
            "{\"id\":7,\"name\":null,\"level\":12}",
            "{\"id\":7,\"name\":42,\"level\":12}",
            "{\"id\":7,\"name\":\"\",\"level\":12}",
            "{\"id\":7,\"name\":\"\u2003\",\"level\":12}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":0}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":101}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":1.5}",
            "{\"id\":7,\"name\":\"Лис\",\"level\":\"12\"}"
    })
    void rejectsInvalid200Response(String invalidBody) {
        body.set(invalidBody);
        assertThrows(IllegalStateException.class, () -> new PlayerProfileClient(baseUri).find(7));
    }

    @Test
    void keepsMalformedJsonAsCause() {
        body.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerProfileClient(baseUri).find(7));
        assertInstanceOf(JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class, () -> new PlayerProfileClient(baseUri).find(7));
    }
}
