package learning.task046;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.StandardCharsets;

public class PlayerRegistrationClient {
    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlayerRegistrationClient(URI baseUri) {
        if (baseUri == null) throw new IllegalArgumentException();
        if (!"http".equalsIgnoreCase(baseUri.getScheme())
                &&  !"https".equalsIgnoreCase(baseUri.getScheme())
                || baseUri.getHost() == null
                || baseUri.getPath() == null
                || !baseUri.getPath().endsWith("/"))
            throw new IllegalArgumentException();
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public int register(String name, int level) throws IOException, InterruptedException {
        if (name == null || name.isBlank() || level < 1 || level > 100) throw new IllegalArgumentException();
        ObjectNode body = mapper.createObjectNode();
        body.put("name", name);
        body.put("level", level);
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("players"))
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
        try {
            if (response.statusCode() == 201) {
                JsonNode root = mapper.readTree(response.body().toString());
                JsonNode idNode = root.path("id");
                if (idNode.isNull() || !idNode.canConvertToInt() || !idNode.isIntegralNumber()) {
                    throw new IllegalStateException();
                }
                int id = idNode.asInt();
                if (id < 1) throw new IllegalStateException();
                return id;
            } else {
                throw new IllegalStateException(String.valueOf(response.statusCode()));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
