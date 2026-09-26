package learning.task050;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PlayerSettingsClient {
    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlayerSettingsClient(URI baseUri) {
        if (baseUri == null || !baseUri.isAbsolute() || !"http".equalsIgnoreCase(baseUri.getScheme())
                && !"https".equalsIgnoreCase(baseUri.getScheme()) || baseUri.getPath() == null
                || !baseUri.getPath().endsWith("/") || baseUri.getHost() == null) {
            throw new IllegalArgumentException();
        }
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public Optional<PlayerSettings> find(int playerId) throws IOException, InterruptedException {
        if (playerId < 1) throw new IllegalArgumentException();
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("players/"+playerId+"/settings"))
                .setHeader("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            try {
                JsonNode root = mapper.readTree(response.body());
                JsonNode playerIdNode =  root.path("playerId");
                JsonNode notifyNode =  root.path("notificationsEnabled");
                JsonNode themeNode =  root.path("theme");

                if (!playerIdNode.isIntegralNumber() || !playerIdNode.canConvertToInt() || !notifyNode.isBoolean()
                        || !themeNode.isTextual()) throw new IllegalStateException();

                int jsonPlayerId = playerIdNode.asInt();
                boolean notify = notifyNode.asBoolean();
                String theme = themeNode.asText();

                if (theme.isBlank() || jsonPlayerId != playerId) throw new IllegalStateException();

                return Optional.of(new PlayerSettings(jsonPlayerId, notify, theme));

            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        } else if (response.statusCode() == 404) {
            return Optional.empty();
        } else {
            throw new IllegalStateException("Unexpected response code " + response.statusCode());
        }
    }
}
