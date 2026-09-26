package learning.task051;

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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchInvitationClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger status = new AtomicInteger(201);
    private final AtomicReference<String> body = new AtomicReference<>(
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":9,\"message\":\"Привет!\"}");
    private final AtomicReference<String> responseContentType = new AtomicReference<>("application/json; charset=UTF-8");
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
    void sendsJsonAndReturnsTypedInvitation() throws Exception {
        assertEquals(Optional.of(new MatchInvitation(3, 7, 9, "Привет!")),
                new MatchInvitationClient(baseUri).send(7, 9, "Привет!"));
        assertEquals(1, requests.get());
        assertEquals("/api/invitations", path.get());
        assertEquals("POST", method.get());
        assertEquals("application/json", accept.get());
        assertEquals("application/json; charset=UTF-8", contentType.get());
        JsonNode json = mapper.readTree(requestBody.get());
        assertTrue(json.isObject());
        assertEquals(3, json.size());
        assertEquals(7, json.path("senderId").intValue());
        assertEquals(9, json.path("recipientId").intValue());
        assertEquals("Привет!", json.path("message").textValue());
    }

    @Test
    void escapesSpecialCharactersAndPreservesOriginalMessage() throws Exception {
        String message = "Кот \"рыцарь\" \\ арена\n";
        body.set(mapper.writeValueAsString(Map.of("invitationId", 1, "senderId", 1,
                "recipientId", 2, "message", message)));
        assertEquals(Optional.of(new MatchInvitation(1, 1, 2, message)),
                new MatchInvitationClient(baseUri).send(1, 2, message));
        assertEquals(message, mapper.readTree(requestBody.get()).path("message").textValue());
    }

    @Test
    void keepsNonBlankWhitespaceAndIgnoresExtraResponseFields() throws Exception {
        body.set("{\"extra\":true,\"message\":\"  бой  \",\"recipientId\":9,\"senderId\":7,\"invitationId\":3}");
        assertEquals(Optional.of(new MatchInvitation(3, 7, 9, "  бой  ")),
                new MatchInvitationClient(baseUri).send(7, 9, "  бой  "));
        assertEquals("  бой  ", mapper.readTree(requestBody.get()).path("message").textValue());
    }

    @Test
    void acceptsLargestIntAndReadsUtf8RegardlessOfResponseCharset() throws Exception {
        responseContentType.set("application/json; charset=ISO-8859-1");
        body.set("{\"invitationId\":2147483647,\"senderId\":1,\"recipientId\":2147483647,\"message\":\"Бой\"}");
        assertEquals(Optional.of(new MatchInvitation(Integer.MAX_VALUE, 1, Integer.MAX_VALUE, "Бой")),
                new MatchInvitationClient(baseUri).send(1, Integer.MAX_VALUE, "Бой"));
    }

    @Test
    void returnsEmptyOnConflictWithoutParsingBody() throws Exception {
        status.set(409);
        body.set("Already invited");
        assertEquals(Optional.empty(), new MatchInvitationClient(baseUri).send(7, 9, "Привет!"));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 500})
    void rejectsOtherStatusesBeforeParsingBody(int code) {
        status.set(code);
        body.set("Not JSON");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new MatchInvitationClient(baseUri).send(7, 9, "Привет!"));
        assertTrue(error.getMessage().contains(String.valueOf(code)));
    }

    @Test
    void validatesArgumentsBeforeRequest() {
        MatchInvitationClient client = new MatchInvitationClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.send(0, 9, "Бой"));
        assertThrows(IllegalArgumentException.class, () -> client.send(7, -1, "Бой"));
        assertThrows(IllegalArgumentException.class, () -> client.send(7, 7, "Бой"));
        assertThrows(IllegalArgumentException.class, () -> client.send(7, 9, null));
        assertThrows(IllegalArgumentException.class, () -> client.send(7, 9, ""));
        assertThrows(IllegalArgumentException.class, () -> client.send(7, 9, "  \u2003  "));
        assertEquals(0, requests.get());
    }

    @Test
    void validatesBaseUri() {
        assertThrows(IllegalArgumentException.class, () -> new MatchInvitationClient(null));
        assertThrows(IllegalArgumentException.class, () -> new MatchInvitationClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new MatchInvitationClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new MatchInvitationClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new MatchInvitationClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "42", "{}",
            "{\"senderId\":7,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":7,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":9}",
            "{\"invitationId\":0,\"senderId\":7,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":2147483648,\"senderId\":7,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":1.5,\"senderId\":7,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":\"3\",\"senderId\":7,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":0,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":8,\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":0,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":8,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":\"7\",\"recipientId\":9,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":9.0,\"message\":\"Привет!\"}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":9,\"message\":null}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":9,\"message\":\"Не привет\"}",
            "{\"invitationId\":3,\"senderId\":7,\"recipientId\":9,\"message\":1}"
    })
    void rejectsInvalidCreatedResponse(String invalidBody) {
        body.set(invalidBody);
        assertThrows(IllegalStateException.class,
                () -> new MatchInvitationClient(baseUri).send(7, 9, "Привет!"));
    }

    @Test
    void preservesMalformedJsonCause() {
        body.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new MatchInvitationClient(baseUri).send(7, 9, "Привет!"));
        assertInstanceOf(JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class,
                () -> new MatchInvitationClient(baseUri).send(7, 9, "Привет!"));
    }
}
