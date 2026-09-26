package learning.task053;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerProfileHandler implements HttpHandler {

    private final Map<Integer, String> playerNames = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();


    public PlayerProfileHandler(Map<Integer, String> playerNames) {
        if (playerNames == null) throw new IllegalArgumentException("playerNames cannot be null");
        this.playerNames.putAll(playerNames);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Pattern pattern = Pattern.compile("^/players/(\\d+)$");
            Matcher matcher = pattern.matcher(exchange.getRequestURI().getPath());

            if (!matcher.matches()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            try {
                int parseInt = Integer.parseInt(matcher.group(1));

                if (parseInt < 1) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                if (!playerNames.containsKey(parseInt)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                String name = playerNames.get(parseInt);

                ObjectNode node = mapper.createObjectNode();
                node.put("playerId", parseInt);
                node.put("name", name);
                String json = mapper.writeValueAsString(node);

                byte[] body  = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);


            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, -1);
            }

        } finally {
            exchange.close();
        }
    }
}
