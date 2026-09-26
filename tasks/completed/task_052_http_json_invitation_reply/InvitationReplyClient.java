package learning.task052;

import com.fasterxml.jackson.core.JsonParseException;
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

public class InvitationReplyClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;
    private final URI baseUri;

    public InvitationReplyClient(URI baseUri) {
        if (baseUri == null || !baseUri.isAbsolute()
                || !"http".equalsIgnoreCase(baseUri.getScheme())
                && !"https".equalsIgnoreCase(baseUri.getScheme()) || baseUri.getHost() == null
                || baseUri.getPath() == null || !baseUri.getPath().endsWith("/")) throw new IllegalArgumentException();
        this.client = HttpClient.newHttpClient();
        this.baseUri = baseUri;
    }

    public Optional<InvitationReply> reply(int invitationId, boolean accepted)
            throws IOException, InterruptedException {
        if (invitationId < 1) throw new IllegalArgumentException();
        ObjectNode node = mapper.createObjectNode();
        node.put("accepted", accepted);
        String json = mapper.writeValueAsString(node);
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("invitations/"+invitationId+"/reply"))
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            try {
                JsonNode root =  mapper.readTree(response.body());
                JsonNode invitationIdNode =  root.path("invitationId");
                JsonNode acceptedNode =  root.path("accepted");
                JsonNode statusNode =  root.path("status");

                if (!invitationIdNode.isIntegralNumber() || !invitationIdNode.canConvertToInt()
                        || !acceptedNode.isBoolean() || !statusNode.isTextual()) throw new IllegalStateException();

                int entryInvitationId = invitationIdNode.asInt();
                boolean entryAccepted = acceptedNode.asBoolean();
                String entryStatus = statusNode.asText();

                if (entryInvitationId != invitationId || entryAccepted != accepted || entryStatus.isBlank())
                    throw new IllegalStateException();

                if (entryAccepted && entryStatus.equals("принято")
                        || !entryAccepted && entryStatus.equals("отклонено"))
                    return Optional.of(new InvitationReply(entryInvitationId, entryAccepted, entryStatus));

                throw new IllegalStateException();
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
