package learning.task054;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ScoreSubmissionHandler implements HttpHandler {

    private final Map<Integer, Integer> bestScores;
    private final ObjectMapper mapper = new ObjectMapper();

    public ScoreSubmissionHandler(Map<Integer, Integer> bestScores) {
        if (bestScores == null) throw new IllegalArgumentException();
        this.bestScores = bestScores;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            if (!"/scores".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] input = exchange.getRequestBody().readAllBytes();
            try {
                JsonNode root = mapper.readTree(input);
                JsonNode idNode = root.path("playerId");
                JsonNode scoreNode = root.path("score");

                if (!idNode.isIntegralNumber() || !idNode.canConvertToInt()
                        || !scoreNode.isIntegralNumber() || !scoreNode.canConvertToInt()) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                int playerId = idNode.asInt();
                int score = scoreNode.asInt();

                if (playerId < 1 || score < 0 || score > 1000) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                if (!bestScores.containsKey(playerId)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                int bestScore = Math.max(bestScores.get(playerId), score);

                boolean improved = false;

                if (bestScore > bestScores.get(playerId)) {
                    bestScores.put(playerId, bestScore);
                    improved = true;
                }

                ObjectNode nodeResponse = mapper.createObjectNode();
                nodeResponse.put("playerId",  playerId);
                nodeResponse.put("bestScore", bestScore);
                nodeResponse.put("improved", improved);
                String json = mapper.writeValueAsString(nodeResponse);
                byte[] output =  json.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, output.length);
                exchange.getResponseBody().write(output);

            } catch (JsonProcessingException e) {
                exchange.sendResponseHeaders(400, -1);
            }

        } finally {
            exchange.close();
        }
    }
}
