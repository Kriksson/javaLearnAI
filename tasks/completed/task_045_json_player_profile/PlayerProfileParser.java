package learning.task045;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PlayerProfileParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public PlayerProfile parse(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            JsonNode idNode = root.path("id");
            if (!idNode.isIntegralNumber() || !idNode.canConvertToInt()) {
                throw new IllegalArgumentException();
            }
            int id = idNode.asInt();
            if (id < 1) throw new IllegalArgumentException();

            JsonNode nameNode = root.path("name");
            if (!nameNode.isTextual()) throw new IllegalArgumentException();
            String name = nameNode.asText();
            if (name.isBlank()) throw new IllegalArgumentException();

            JsonNode levelNode = root.path("level");
            if (!levelNode.isIntegralNumber() || !levelNode.canConvertToInt()) {
                throw new IllegalArgumentException();
            }
            int level = levelNode.asInt();
            if (level < 1 || level > 100) throw new IllegalArgumentException();

            return new PlayerProfile(id, name, level);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
