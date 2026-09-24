package learning.task044;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerApiClientTest {
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{\"id\":7,\"name\":\"Лис\"}");
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> acceptHeader = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            requestPath.set(exchange.getRequestURI().getPath());
            requestMethod.set(exchange.getRequestMethod());
            acceptHeader.set(exchange.getRequestHeaders().getFirst("Accept"));
            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
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
    void fetchesUtf8JsonWithGetAndAcceptHeader() throws Exception {
        Optional<String> result = new PlayerApiClient(baseUri).fetchPlayerJson(7);

        assertEquals(Optional.of("{\"id\":7,\"name\":\"Лис\"}"), result);
        assertEquals(1, requests.get());
        assertEquals("/api/players/7", requestPath.get());
        assertEquals("GET", requestMethod.get());
        assertEquals("application/json", acceptHeader.get());
    }

    @Test
    void returnsEmptyWhenPlayerIsNotFound() throws Exception {
        responseStatus.set(404);
        assertEquals(Optional.empty(), new PlayerApiClient(baseUri).fetchPlayerJson(999));
        assertEquals("/api/players/999", requestPath.get());
    }

    @Test
    void keepsEmptySuccessfulBody() throws Exception {
        responseBody.set("");
        assertEquals(Optional.of(""), new PlayerApiClient(baseUri).fetchPlayerJson(1));
    }

    @Test
    void rejectsServerFailureWithStatusInMessage() {
        responseStatus.set(500);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerApiClient(baseUri).fetchPlayerJson(7));
        assertTrue(error.getMessage().contains("500"));
    }

    @Test
    void rejectsUnexpectedSuccessfulStatus() {
        responseStatus.set(201);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerApiClient(baseUri).fetchPlayerJson(7));
        assertTrue(error.getMessage().contains("201"));
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class, () -> new PlayerApiClient(baseUri).fetchPlayerJson(7));
    }

    @Test
    void rejectsNonPositiveIdsBeforeRequest() {
        PlayerApiClient client = new PlayerApiClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.fetchPlayerJson(0));
        assertThrows(IllegalArgumentException.class, () -> client.fetchPlayerJson(-1));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUris() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerApiClient(null));
        assertThrows(IllegalArgumentException.class, () -> new PlayerApiClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerApiClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerApiClient(URI.create("http://localhost:8080/api")));
        assertEquals(0, requests.get());
    }
}
