package learning.task054;

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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreSubmissionHandlerTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private Map<Integer, Integer> bestScores;
    private String host;

    @BeforeEach
    void startServer() throws IOException {
        bestScores = new HashMap<>(Map.of(7, 100, 1, 0, Integer.MAX_VALUE, 1000));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/scores", new ScoreSubmissionHandler(bestScores));
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
    void improvesRecordAndReturnsTypedJson() throws Exception {
        HttpResponse<byte[]> response = send("POST", "/scores", "{\"playerId\":7,\"score\":120}");
        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(null));
        assertEquals(response.body().length,
                Integer.parseInt(response.headers().firstValue("Content-Length").orElseThrow()));
        JsonNode json = mapper.readTree(response.body());
        assertTrue(json.isObject());
        assertEquals(3, json.size());
        assertTrue(json.path("playerId").isIntegralNumber());
        assertEquals(7, json.path("playerId").intValue());
        assertTrue(json.path("bestScore").isIntegralNumber());
        assertEquals(120, json.path("bestScore").intValue());
        assertTrue(json.path("improved").isBoolean());
        assertTrue(json.path("improved").booleanValue());
        assertEquals(120, bestScores.get(7));
    }

    @Test
    void lowerScoreLeavesRecordUnchanged() throws Exception {
        HttpResponse<byte[]> response = send("POST", "/scores", "{\"score\":80,\"playerId\":7,\"extra\":\"Кот\"}");
        assertEquals(200, response.statusCode());
        JsonNode json = mapper.readTree(response.body());
        assertEquals(100, json.path("bestScore").intValue());
        assertTrue(json.path("improved").isBoolean());
        assertEquals(false, json.path("improved").booleanValue());
        assertEquals(100, bestScores.get(7));
    }

    @Test
    void equalScoreDoesNotCountAsImprovement() throws Exception {
        HttpResponse<byte[]> response = send("POST", "/scores", "{\"playerId\":7,\"score\":100}");
        assertEquals(200, response.statusCode());
        assertEquals(false, mapper.readTree(response.body()).path("improved").booleanValue());
        assertEquals(100, bestScores.get(7));
    }

    @Test
    void acceptsZeroAndUpperScoreBoundAndLargestPlayerId() throws Exception {
        HttpResponse<byte[]> zero = send("POST", "/scores", "{\"playerId\":1,\"score\":0}");
        assertEquals(200, zero.statusCode());
        assertEquals(0, mapper.readTree(zero.body()).path("bestScore").intValue());
        assertEquals(false, mapper.readTree(zero.body()).path("improved").booleanValue());

        HttpResponse<byte[]> upper = send("POST", "/scores", "{\"playerId\":1,\"score\":1000}");
        assertEquals(200, upper.statusCode());
        assertEquals(1000, mapper.readTree(upper.body()).path("bestScore").intValue());
        assertEquals(true, mapper.readTree(upper.body()).path("improved").booleanValue());
        assertEquals(1000, bestScores.get(1));

        HttpResponse<byte[]> maxId = send("POST", "/scores",
                "{\"playerId\":2147483647,\"score\":1000}");
        assertEquals(200, maxId.statusCode());
        assertEquals(Integer.MAX_VALUE, mapper.readTree(maxId.body()).path("playerId").intValue());
    }

    @Test
    void unknownPlayerReturns404WithoutChangingMap() throws Exception {
        HttpResponse<byte[]> response = send("POST", "/scores", "{\"playerId\":8,\"score\":120}");
        assertEquals(404, response.statusCode());
        assertEquals(0, response.body().length);
        assertEquals(Map.of(7, 100, 1, 0, Integer.MAX_VALUE, 1000), bestScores);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "{broken", "null", "[]", "42", "{}",
            "{\"score\":100}",
            "{\"playerId\":7}",
            "{\"playerId\":0,\"score\":100}",
            "{\"playerId\":-1,\"score\":100}",
            "{\"playerId\":2147483648,\"score\":100}",
            "{\"playerId\":4294967303,\"score\":100}",
            "{\"playerId\":7.0,\"score\":100}",
            "{\"playerId\":\"7\",\"score\":100}",
            "{\"playerId\":true,\"score\":100}",
            "{\"playerId\":7,\"score\":-1}",
            "{\"playerId\":7,\"score\":1001}",
            "{\"playerId\":7,\"score\":4294967396}",
            "{\"playerId\":7,\"score\":1.5}",
            "{\"playerId\":7,\"score\":\"100\"}",
            "{\"playerId\":7,\"score\":null}"
    })
    void invalidJsonReturns400WithoutChangingMap(String body) throws Exception {
        HttpResponse<byte[]> response = send("POST", "/scores", body);
        assertEquals(400, response.statusCode());
        assertEquals(0, response.body().length);
        assertEquals(Map.of(7, 100, 1, 0, Integer.MAX_VALUE, 1000), bestScores);
    }

    @Test
    void wrongPathReturns404BeforeParsingBody() throws Exception {
        HttpResponse<byte[]> response = send("POST", "/scores/extra", "{broken");
        assertEquals(404, response.statusCode());
        assertEquals(0, response.body().length);
        assertEquals(100, bestScores.get(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "post"})
    void wrongMethodReturns405BeforePathAndBody(String method) throws Exception {
        HttpResponse<byte[]> response = send(method, "/scores/extra", "{broken");
        assertEquals(405, response.statusCode());
        assertEquals("POST", response.headers().firstValue("Allow").orElse(null));
        assertEquals(0, response.body().length);
        assertEquals(100, bestScores.get(7));
    }

    @Test
    void rejectsNullMap() {
        assertThrows(IllegalArgumentException.class, () -> new ScoreSubmissionHandler(null));
    }

    private HttpResponse<byte[]> send(String method, String path, String body)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(host + path))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json; charset=UTF-8")
                .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
