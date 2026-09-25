package learning.task047;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PlayerProfileClient {
    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlayerProfileClient(URI baseUri) {
        if(baseUri == null
                || !baseUri.isAbsolute()
                || !"http".equalsIgnoreCase(baseUri.getScheme())
                && !"https".equalsIgnoreCase(baseUri.getScheme())
                || baseUri.getHost() == null
                || baseUri.getPath() == null
                || !baseUri.getPath().endsWith("/"))
            throw new IllegalArgumentException();
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public Optional<PlayerProfile> find(int playerId) throws IOException, InterruptedException {
        if (playerId < 1) throw new IllegalArgumentException();
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("players/"+playerId)).setHeader("Accept", "application/json").GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            try {
                JsonNode root = mapper.readTree(response.body());
                JsonNode idNode = root.path("id");
                JsonNode nameNode = root.path("name");
                JsonNode levelNode = root.path("level");

                if (!idNode.isIntegralNumber()
                        || !idNode.canConvertToInt()
                        || !nameNode.isTextual()
                        || !levelNode.isIntegralNumber()
                        || !levelNode.canConvertToInt()) {
                    throw new IllegalStateException();
                }

                String name = nameNode.asText();
                int id = idNode.asInt();
                int level = levelNode.asInt();

                if (level < 1 || level > 100) throw new IllegalStateException();
                if (id < 1) throw new IllegalStateException();
                if (name.isBlank()) throw new IllegalStateException();
                return Optional.of(new PlayerProfile(id, name, level));
            } catch (JsonParseException e) {
                throw new IllegalStateException(e);
            }
        } else if (response.statusCode() == 404) {
            return Optional.empty();
        } else {
            throw new IllegalStateException(""+response.statusCode());
        }
    }
}
