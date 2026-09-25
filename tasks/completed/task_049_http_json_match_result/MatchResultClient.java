package learning.task049;

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

public class MatchResultClient {
    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public MatchResultClient(URI baseUri) {
        if (baseUri == null
                ||  !baseUri.isAbsolute()
                || !"http".equalsIgnoreCase(baseUri.getScheme())
                && !"https".equalsIgnoreCase(baseUri.getScheme())
                || baseUri.getHost() == null
                || baseUri.getPath() == null
                || !baseUri.getPath().endsWith("/"))
                    throw new IllegalArgumentException();
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public Optional<MatchResult> find(int entryId) throws IOException, InterruptedException {
        if (entryId < 1) throw new IllegalArgumentException();
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("scores/"+entryId))
                .setHeader("Accept", "application/json")
                .GET()
                .build();
        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            try {
                JsonNode root = mapper.readTree(response.body().toString());
                JsonNode entryIdNode = root.path("entryId");
                JsonNode matchNameNode = root.path("matchName");
                JsonNode pointsNode = root.path("points");

                if (!entryIdNode.isIntegralNumber() || !entryIdNode.canConvertToInt()
                    || !matchNameNode.isTextual()
                    || !pointsNode.isIntegralNumber() || !pointsNode.canConvertToInt()) throw new IllegalStateException();

                int id =  entryIdNode.asInt();
                String matchName =  matchNameNode.asText();
                int points = pointsNode.asInt();
                if (id < 1 || matchName.isBlank() || points < 0) throw new IllegalStateException();
                if (id == entryId) return Optional.of(new MatchResult(id, matchName, points));
                throw new IllegalStateException();
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        } else if (response.statusCode() == 404) {
            return Optional.empty();
        } else {
            throw new IllegalStateException(""+response.statusCode());
        }
    }
}
