package learning.task048;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class ScoreSubmissionClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger responseStatus = new AtomicInteger(201);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{\"entryId\":42}");
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> accept = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            path.set(exchange.getRequestURI().getPath());
            method.set(exchange.getRequestMethod());
            accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus.get(), bytes.length);
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
    void sendsPostWithJsonAndReturnsEntryId() throws Exception {
        assertEquals(Optional.of(42), new ScoreSubmissionClient(baseUri).submit(7, "Финал", 120));
        assertEquals(1, requests.get());
        assertEquals("/api/scores", path.get());
        assertEquals("POST", method.get());
        assertEquals("application/json", accept.get());
        assertEquals("application/json; charset=UTF-8", contentType.get());
        JsonNode json = mapper.readTree(requestBody.get());
        assertEquals(3, json.size());
        assertTrue(json.path("playerId").isIntegralNumber());
        assertEquals(7, json.path("playerId").intValue());
        assertEquals("Финал", json.path("matchName").textValue());
        assertTrue(json.path("points").isIntegralNumber());
        assertEquals(120, json.path("points").intValue());
    }

    @Test
    void preservesNameWithQuotesBackslashNewlineAndSpaces() throws Exception {
        String name = " Финал \\\"\n ";
        assertEquals(Optional.of(42), new ScoreSubmissionClient(baseUri).submit(1, name, 0));
        JsonNode json = mapper.readTree(requestBody.get());
        assertEquals(name, json.path("matchName").textValue());
        assertEquals(0, json.path("points").intValue());
    }

    @Test
    void acceptsMaximumInputAndResponseIds() throws Exception {
        responseBody.set("{\"entryId\":2147483647,\"extra\":true}");
        assertEquals(Optional.of(Integer.MAX_VALUE),
                new ScoreSubmissionClient(baseUri).submit(Integer.MAX_VALUE, "Матч", Integer.MAX_VALUE));
    }

    @Test
    void returnsEmptyOnConflictWithoutParsingTextBody() throws Exception {
        responseStatus.set(409);
        responseBody.set("Already exists");
        assertEquals(Optional.empty(), new ScoreSubmissionClient(baseUri).submit(7, "Финал", 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 503})
    void reportsOtherStatusesBeforeReadingBody(int code) {
        responseStatus.set(code);
        responseBody.set("Unavailable");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new ScoreSubmissionClient(baseUri).submit(7, "Финал", 120));
        assertTrue(error.getMessage().contains(String.valueOf(code)));
    }

    @Test
    void rejectsInvalidInputsWithoutRequest() {
        ScoreSubmissionClient client = new ScoreSubmissionClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.submit(0, "Финал", 0));
        assertThrows(IllegalArgumentException.class, () -> client.submit(-1, "Финал", 0));
        assertThrows(IllegalArgumentException.class, () -> client.submit(1, null, 0));
        assertThrows(IllegalArgumentException.class, () -> client.submit(1, "", 0));
        assertThrows(IllegalArgumentException.class, () -> client.submit(1, "\u2003", 0));
        assertThrows(IllegalArgumentException.class, () -> client.submit(1, "Финал", -1));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUris() {
        assertThrows(IllegalArgumentException.class, () -> new ScoreSubmissionClient(null));
        assertThrows(IllegalArgumentException.class, () -> new ScoreSubmissionClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new ScoreSubmissionClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new ScoreSubmissionClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new ScoreSubmissionClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "42", "{}", "{\"entryId\":null}",
            "{\"entryId\":0}", "{\"entryId\":-1}",
            "{\"entryId\":2147483648}", "{\"entryId\":1.5}",
            "{\"entryId\":\"42\"}"
    })
    void rejectsInvalid201Responses(String invalidBody) {
        responseBody.set(invalidBody);
        assertThrows(IllegalStateException.class,
                () -> new ScoreSubmissionClient(baseUri).submit(7, "Финал", 120));
    }

    @Test
    void preservesMalformedJsonCause() {
        responseBody.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new ScoreSubmissionClient(baseUri).submit(7, "Финал", 120));
        assertInstanceOf(JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class,
                () -> new ScoreSubmissionClient(baseUri).submit(7, "Финал", 120));
    }
}
