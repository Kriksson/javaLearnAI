package learning.task053;

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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileHandlerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;
    private String host;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/players/", new PlayerProfileHandler(Map.of(
                1, "Alice",
                7, "Кот \"Рыцарь\" \\",
                Integer.MAX_VALUE, "Max")));
        server.start();
        host = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsPlayerAsTypedJson() throws Exception {
        HttpResponse<byte[]> response = send("GET", "/players/1");
        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(null));
        JsonNode json = mapper.readTree(response.body());
        assertTrue(json.isObject());
        assertEquals(2, json.size());
        assertTrue(json.path("playerId").isIntegralNumber());
        assertEquals(1, json.path("playerId").intValue());
        assertTrue(json.path("name").isTextual());
        assertEquals("Alice", json.path("name").textValue());
    }

    @Test
    void keepsUtf8AndEscapesNameCorrectly() throws Exception {
        HttpResponse<byte[]> response = send("GET", "/players/7");
        assertEquals(200, response.statusCode());
        assertEquals(response.body().length,
                Integer.parseInt(response.headers().firstValue("Content-Length").orElseThrow()));
        JsonNode json = mapper.readTree(new String(response.body(), StandardCharsets.UTF_8));
        assertEquals("Кот \"Рыцарь\" \\", json.path("name").textValue());
        assertEquals(7, json.path("playerId").intValue());
    }

    @Test
    void acceptsLeadingZerosAndLargestInt() throws Exception {
        HttpResponse<byte[]> withZeros = send("GET", "/players/007");
        assertEquals(200, withZeros.statusCode());
        assertEquals(7, mapper.readTree(withZeros.body()).path("playerId").intValue());

        HttpResponse<byte[]> max = send("GET", "/players/2147483647");
        assertEquals(200, max.statusCode());
        assertEquals(Integer.MAX_VALUE, mapper.readTree(max.body()).path("playerId").intValue());
    }

    @Test
    void returns404ForUnknownPositiveIdWithoutBody() throws Exception {
        HttpResponse<byte[]> response = send("GET", "/players/2");
        assertEquals(404, response.statusCode());
        assertEquals(0, response.body().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/players/", "/players/0", "/players/000", "/players/-1",
            "/players/+1", "/players/abc", "/players/1a", "/players/1/extra",
            "/players/2147483648", "/players/999999999999999999999999"
    })
    void returns400ForInvalidPathWithoutBody(String path) throws Exception {
        HttpResponse<byte[]> response = send("GET", path);
        assertEquals(400, response.statusCode());
        assertEquals(0, response.body().length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void returns405ForUnsupportedMethodBeforeCheckingPath(String method) throws Exception {
        HttpResponse<byte[]> response = send(method, "/players/not-a-number");
        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("Allow").orElse(null));
        assertEquals(0, response.body().length);
    }

    @Test
    void rejectsNullMap() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerProfileHandler(null));
    }

    private HttpResponse<byte[]> send(String method, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(host + path))
                .timeout(Duration.ofSeconds(3))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
