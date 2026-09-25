package learning.task048;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ScoreSubmissionClient {
    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public ScoreSubmissionClient(URI baseUri) {
        if (baseUri == null
                || !baseUri.isAbsolute()
                || !"http".equalsIgnoreCase(baseUri.getScheme())
                && !"https".equalsIgnoreCase(baseUri.getScheme())
                || baseUri.getHost() == null
                || baseUri.getPath() == null
                || !baseUri.getPath().endsWith("/")) throw new IllegalArgumentException();
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public Optional<Integer> submit(int playerId, String matchName, int points)
            throws IOException, InterruptedException {
        if (playerId < 1 || matchName == null || matchName.isBlank() || points < 0) throw new IllegalArgumentException();
        ObjectNode body = mapper.createObjectNode();
        body.put("playerId", playerId);
        body.put("matchName", matchName);
        body.put("points", points);
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("scores"))
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setHeader("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
        try {
            if (response.statusCode() == 201) {
                JsonNode root = mapper.readTree(response.body().toString());
                JsonNode entryIdNode = root.path("entryId");

                if (!entryIdNode.isIntegralNumber() || !entryIdNode.canConvertToInt()) {
                    throw new IllegalStateException();
                }

                int id = entryIdNode.asInt();
                if (id < 1) throw new IllegalStateException();
                return Optional.of(id);
            } else if (response.statusCode() == 409) {
                return Optional.empty();
            } else {
                throw new IllegalStateException("" + response.statusCode());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
