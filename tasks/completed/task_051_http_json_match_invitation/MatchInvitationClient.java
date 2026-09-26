package learning.task051;

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

public class MatchInvitationClient {

    private final URI baseUri;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;

    public MatchInvitationClient(URI baseUri) {
        if (baseUri == null || !baseUri.isAbsolute()
                || !"http".equalsIgnoreCase(baseUri.getScheme()) && !"https".equalsIgnoreCase(baseUri.getScheme())
                || baseUri.getHost() == null || baseUri.getPath() == null || !baseUri.getPath().endsWith("/"))
            throw new IllegalArgumentException();
        this.baseUri = baseUri;
        this.client = HttpClient.newHttpClient();
    }

    public Optional<MatchInvitation> send(int senderId, int recipientId, String message)
            throws IOException, InterruptedException {
        if (senderId < 1 || recipientId < 1 || senderId == recipientId || message == null || message.isBlank())
            throw new IllegalArgumentException();
        ObjectNode body = mapper.createObjectNode();
        body.put("senderId", senderId);
        body.put("recipientId", recipientId);
        body.put("message", message);
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("invitations"))
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 201) {
            try {
                JsonNode root = mapper.readTree(response.body());
                JsonNode invitationIdNode =  root.path("invitationId");
                JsonNode senderIdNode = root.path("senderId");
                JsonNode recipientIdNode = root.path("recipientId");
                JsonNode messageNode = root.path("message");

                if (!invitationIdNode.isIntegralNumber() || !invitationIdNode.canConvertToInt()
                        || !senderIdNode.isIntegralNumber() || !senderIdNode.canConvertToInt()
                        || !recipientIdNode.isIntegralNumber() || !recipientIdNode.canConvertToInt()
                        || !messageNode.isTextual()) throw new IllegalStateException();

                int entryInvitationId = invitationIdNode.asInt();
                int entrySenderId = senderIdNode.asInt();
                int entryRecipientId = recipientIdNode.asInt();
                String entryMessage = messageNode.asText();

                if (entryInvitationId < 1 || entrySenderId != senderId || entryRecipientId != recipientId
                        || !entryMessage.equals(message)) throw new IllegalStateException();

                return Optional.of(new MatchInvitation(entryInvitationId, entrySenderId, entryRecipientId, entryMessage));

            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        } else if (response.statusCode() == 409) {
            return Optional.empty();
        } else {
            throw new IllegalStateException(""+response.statusCode());
        }
    }
}
