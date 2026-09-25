package learning.task046;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRegistrationClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger responseStatus = new AtomicInteger(201);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{\"id\":42}");
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> accept = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private HttpServer server;
    private URI baseUri;

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
    void sendsPostWithHeadersAndJsonAndReturnsId() throws Exception {
        assertEquals(42, new PlayerRegistrationClient(baseUri).register("Лис", 12));
        assertEquals(1, requests.get());
        assertEquals("/api/players", path.get());
        assertEquals("POST", method.get());
        assertEquals("application/json", accept.get());
        assertEquals("application/json; charset=UTF-8", contentType.get());
        JsonNode body = mapper.readTree(requestBody.get());
        assertEquals(2, body.size());
        assertEquals("Лис", body.path("name").textValue());
        assertTrue(body.path("level").isIntegralNumber());
        assertEquals(12, body.path("level").intValue());
    }

    @Test
    void escapesSpecialCharactersAndKeepsNameWhitespace() throws Exception {
        String name = " Лис \\\"\n ";
        assertEquals(42, new PlayerRegistrationClient(baseUri).register(name, 1));
        JsonNode body = mapper.readTree(requestBody.get());
        assertEquals(name, body.path("name").textValue());
        assertEquals(1, body.path("level").intValue());
    }

    @Test
    void acceptsUpperBoundariesAndIgnoresExtraResponseFields() throws Exception {
        responseBody.set("{\"bonus\":true,\"id\":2147483647}");
        assertEquals(Integer.MAX_VALUE, new PlayerRegistrationClient(baseUri).register("Герой", 100));
    }

    @Test
    void rejectsInvalidInputsBeforeRequest() {
        PlayerRegistrationClient client = new PlayerRegistrationClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.register(null, 10));
        assertThrows(IllegalArgumentException.class, () -> client.register("", 10));
        assertThrows(IllegalArgumentException.class, () -> client.register("\u2003", 10));
        assertThrows(IllegalArgumentException.class, () -> client.register("Лис", 0));
        assertThrows(IllegalArgumentException.class, () -> client.register("Лис", 101));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUris() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerRegistrationClient(null));
        assertThrows(IllegalArgumentException.class, () -> new PlayerRegistrationClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerRegistrationClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerRegistrationClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new PlayerRegistrationClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409, 500})
    void rejectsNonCreatedStatuses(int status) {
        responseStatus.set(status);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerRegistrationClient(baseUri).register("Лис", 12));
        assertTrue(error.getMessage().contains(String.valueOf(status)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "{}", "{\"id\":null}", "{\"id\":0}",
            "{\"id\":-1}", "{\"id\":2147483648}", "{\"id\":1.5}", "{\"id\":\"42\"}"
    })
    void rejectsInvalidCreatedResponses(String body) {
        responseBody.set(body);
        assertThrows(IllegalStateException.class,
                () -> new PlayerRegistrationClient(baseUri).register("Лис", 12));
    }

    @Test
    void preservesMalformedJsonCause() {
        responseBody.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new PlayerRegistrationClient(baseUri).register("Лис", 12));
        assertInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class,
                () -> new PlayerRegistrationClient(baseUri).register("Лис", 12));
    }
}
