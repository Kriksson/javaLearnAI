package learning.task044;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PlayerApiClient {
    private final URI baseUri;
    private final HttpClient client;

    public PlayerApiClient(URI baseUri) {
        if (baseUri == null || baseUri.getPath() == null) throw new IllegalArgumentException();
        if (!baseUri.isAbsolute()
                || !"http".equalsIgnoreCase(baseUri.getScheme())
                && !"https".equalsIgnoreCase(baseUri.getScheme())
                || !baseUri.getPath().endsWith("/")
        ) throw new IllegalArgumentException();
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public Optional<String> fetchPlayerJson(int playerId) throws IOException, InterruptedException {
        if (playerId < 1) throw new IllegalArgumentException();
        HttpRequest httpRequest = HttpRequest.newBuilder(baseUri.resolve("players/"+playerId)).setHeader("Accept", "application/json").GET().build();
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            return Optional.of(response.body());
        } else if (response.statusCode() == 404) {
            return Optional.empty();
        } else {
            throw new IllegalStateException(response.statusCode() + " " + response.body());
        }
    }
}
