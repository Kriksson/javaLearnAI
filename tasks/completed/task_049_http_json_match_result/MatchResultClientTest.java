package learning.task049;

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

class MatchResultClientTest {
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("{\"entryId\":18,\"matchName\":\"Финал\",\"points\":120}");
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
    void getsTypedMatchResult() throws Exception {
        assertEquals(Optional.of(new MatchResult(18, "Финал", 120)), new MatchResultClient(baseUri).find(18));
        assertEquals(1, requests.get());
        assertEquals("/api/scores/18", path.get());
        assertEquals("GET", method.get());
        assertEquals("application/json", accept.get());
    }

    @Test
    void keepsNameAndAcceptsZeroPointsAndExtraFields() throws Exception {
        body.set("{\"points\":0,\"extra\":true,\"matchName\":\" Финал \",\"entryId\":1}");
        assertEquals(Optional.of(new MatchResult(1, " Финал ", 0)), new MatchResultClient(baseUri).find(1));
    }

    @Test
    void acceptsUpperIntegerBoundaries() throws Exception {
        body.set("{\"entryId\":2147483647,\"matchName\":\"Матч\",\"points\":2147483647}");
        assertEquals(Optional.of(new MatchResult(Integer.MAX_VALUE, "Матч", Integer.MAX_VALUE)),
                new MatchResultClient(baseUri).find(Integer.MAX_VALUE));
    }

    @Test
    void returnsEmptyOn404BeforeParsingTextBody() throws Exception {
        status.set(404);
        body.set("Not found");
        assertEquals(Optional.empty(), new MatchResultClient(baseUri).find(18));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 503})
    void reportsOtherStatusBeforeParsingTextBody(int code) {
        status.set(code);
        body.set("Unavailable");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new MatchResultClient(baseUri).find(18));
        assertTrue(error.getMessage().contains(String.valueOf(code)));
    }

    @Test
    void rejectsBadRequestIdBeforeNetwork() {
        MatchResultClient client = new MatchResultClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.find(0));
        assertThrows(IllegalArgumentException.class, () -> client.find(-1));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUris() {
        assertThrows(IllegalArgumentException.class, () -> new MatchResultClient(null));
        assertThrows(IllegalArgumentException.class, () -> new MatchResultClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new MatchResultClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new MatchResultClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new MatchResultClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "42", "{}",
            "{\"entryId\":18,\"matchName\":\"Финал\"}",
            "{\"entryId\":18,\"points\":120}",
            "{\"matchName\":\"Финал\",\"points\":120}",
            "{\"entryId\":0,\"matchName\":\"Финал\",\"points\":120}",
            "{\"entryId\":19,\"matchName\":\"Финал\",\"points\":120}",
            "{\"entryId\":2147483648,\"matchName\":\"Финал\",\"points\":120}",
            "{\"entryId\":1.5,\"matchName\":\"Финал\",\"points\":120}",
            "{\"entryId\":\"18\",\"matchName\":\"Финал\",\"points\":120}",
            "{\"entryId\":18,\"matchName\":null,\"points\":120}",
            "{\"entryId\":18,\"matchName\":\"\",\"points\":120}",
            "{\"entryId\":18,\"matchName\":\"\u2003\",\"points\":120}",
            "{\"entryId\":18,\"matchName\":12,\"points\":120}",
            "{\"entryId\":18,\"matchName\":\"Финал\",\"points\":-1}",
            "{\"entryId\":18,\"matchName\":\"Финал\",\"points\":2147483648}",
            "{\"entryId\":18,\"matchName\":\"Финал\",\"points\":1.5}",
            "{\"entryId\":18,\"matchName\":\"Финал\",\"points\":\"120\"}"
    })
    void rejectsInvalid200Responses(String invalidBody) {
        body.set(invalidBody);
        assertThrows(IllegalStateException.class, () -> new MatchResultClient(baseUri).find(18));
    }

    @Test
    void keepsMalformedJsonCause() {
        body.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new MatchResultClient(baseUri).find(18));
        assertInstanceOf(JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class, () -> new MatchResultClient(baseUri).find(18));
    }
}
