package learning.task052;

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

class InvitationReplyClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private URI baseUri;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>(
            "{\"invitationId\":12,\"accepted\":true,\"status\":\"принято\"}");
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
    void sendsTrueAsJsonBooleanAndReturnsReply() throws Exception {
        assertEquals(Optional.of(new InvitationReply(12, true, "принято")),
                new InvitationReplyClient(baseUri).reply(12, true));
        assertEquals(1, requests.get());
        assertEquals("/api/invitations/12/reply", path.get());
        assertEquals("POST", method.get());
        assertEquals("application/json", accept.get());
        assertEquals("application/json; charset=UTF-8", contentType.get());
        JsonNode request = mapper.readTree(requestBody.get());
        assertTrue(request.isObject());
        assertEquals(1, request.size());
        assertTrue(request.path("accepted").isBoolean());
        assertTrue(request.path("accepted").booleanValue());
    }

    @Test
    void sendsFalseAsJsonBooleanAndAcceptsExtraResponseFields() throws Exception {
        body.set("{\"extra\":9,\"status\":\"отклонено\",\"accepted\":false,\"invitationId\":12}");
        assertEquals(Optional.of(new InvitationReply(12, false, "отклонено")),
                new InvitationReplyClient(baseUri).reply(12, false));
        JsonNode request = mapper.readTree(requestBody.get());
        assertTrue(request.path("accepted").isBoolean());
        assertEquals(false, request.path("accepted").booleanValue());
    }

    @Test
    void readsResponseAsUtf8RegardlessOfCharsetHeader() throws Exception {
        responseContentType.set("application/json; charset=ISO-8859-1");
        assertEquals(Optional.of(new InvitationReply(12, true, "принято")),
                new InvitationReplyClient(baseUri).reply(12, true));
    }

    @Test
    void acceptsLargestInvitationId() throws Exception {
        body.set("{\"invitationId\":2147483647,\"accepted\":true,\"status\":\"принято\"}");
        assertEquals(Optional.of(new InvitationReply(Integer.MAX_VALUE, true, "принято")),
                new InvitationReplyClient(baseUri).reply(Integer.MAX_VALUE, true));
        assertEquals("/api/invitations/2147483647/reply", path.get());
    }

    @Test
    void returnsEmptyOn404WithoutParsingBody() throws Exception {
        status.set(404);
        body.set("Not found");
        assertEquals(Optional.empty(), new InvitationReplyClient(baseUri).reply(12, true));
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 400, 409, 500})
    void rejectsOtherStatusesBeforeParsingBody(int code) {
        status.set(code);
        body.set("Not JSON");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new InvitationReplyClient(baseUri).reply(12, true));
        assertTrue(error.getMessage().contains(String.valueOf(code)));
    }

    @Test
    void rejectsNonPositiveIdBeforeRequest() {
        InvitationReplyClient client = new InvitationReplyClient(baseUri);
        assertThrows(IllegalArgumentException.class, () -> client.reply(0, true));
        assertThrows(IllegalArgumentException.class, () -> client.reply(-1, false));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidBaseUri() {
        assertThrows(IllegalArgumentException.class, () -> new InvitationReplyClient(null));
        assertThrows(IllegalArgumentException.class, () -> new InvitationReplyClient(URI.create("/api/")));
        assertThrows(IllegalArgumentException.class, () -> new InvitationReplyClient(URI.create("file:///tmp/api/")));
        assertThrows(IllegalArgumentException.class, () -> new InvitationReplyClient(URI.create("http:/api/")));
        assertThrows(IllegalArgumentException.class, () -> new InvitationReplyClient(URI.create("http://localhost/api")));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "[]", "42", "{}",
            "{\"accepted\":true,\"status\":\"принято\"}",
            "{\"invitationId\":12,\"status\":\"принято\"}",
            "{\"invitationId\":12,\"accepted\":true}",
            "{\"invitationId\":0,\"accepted\":true,\"status\":\"принято\"}",
            "{\"invitationId\":13,\"accepted\":true,\"status\":\"принято\"}",
            "{\"invitationId\":2147483648,\"accepted\":true,\"status\":\"принято\"}",
            "{\"invitationId\":12.5,\"accepted\":true,\"status\":\"принято\"}",
            "{\"invitationId\":\"12\",\"accepted\":true,\"status\":\"принято\"}",
            "{\"invitationId\":12,\"accepted\":false,\"status\":\"принято\"}",
            "{\"invitationId\":12,\"accepted\":\"true\",\"status\":\"принято\"}",
            "{\"invitationId\":12,\"accepted\":1,\"status\":\"принято\"}",
            "{\"invitationId\":12,\"accepted\":null,\"status\":\"принято\"}",
            "{\"invitationId\":12,\"accepted\":true,\"status\":\"отклонено\"}",
            "{\"invitationId\":12,\"accepted\":true,\"status\":\"Принято\"}",
            "{\"invitationId\":12,\"accepted\":true,\"status\":null}",
            "{\"invitationId\":12,\"accepted\":true,\"status\":1}"
    })
    void rejectsInvalid200Response(String invalidBody) {
        body.set(invalidBody);
        assertThrows(IllegalStateException.class,
                () -> new InvitationReplyClient(baseUri).reply(12, true));
    }

    @Test
    void rejectsAcceptedStatusForFalse() {
        body.set("{\"invitationId\":12,\"accepted\":false,\"status\":\"принято\"}");
        assertThrows(IllegalStateException.class,
                () -> new InvitationReplyClient(baseUri).reply(12, false));
    }

    @Test
    void preservesMalformedJsonCause() {
        body.set("{broken");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new InvitationReplyClient(baseUri).reply(12, true));
        assertInstanceOf(JsonProcessingException.class, error.getCause());
    }

    @Test
    void propagatesNetworkFailure() {
        server.stop(0);
        server = null;
        assertThrows(IOException.class, () -> new InvitationReplyClient(baseUri).reply(12, true));
    }
}
