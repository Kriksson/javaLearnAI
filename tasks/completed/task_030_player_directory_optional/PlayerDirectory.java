package learning.task030;

import java.util.*;

public class PlayerDirectory {

    private final Map<String, PlayerProfile> playerProfiles = new HashMap<>();

    public void register(PlayerProfile profile) {
        if (profile == null) throw new IllegalArgumentException();
        if (playerProfiles.containsKey(profile.id())) {
            playerProfiles.replace(profile.id(), profile);
        } else  {
            playerProfiles.put(profile.id(), profile);
        }
    }

    public Optional<PlayerProfile> findById(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException();
        id = id.trim();
        if (playerProfiles.containsKey(id)) return Optional.of(playerProfiles.get(id));
        return Optional.empty();
    }

    public int size() {
        return playerProfiles.size();
    }

    public List<PlayerProfile> withMinimumLevel(int minimumLevel) {
        if (minimumLevel < 1 || minimumLevel > 100) throw new IllegalArgumentException();
        List<PlayerProfile> result = new ArrayList<>();
        for (PlayerProfile profile : playerProfiles.values()) {
            if (profile.level() >= minimumLevel) result.add(profile);
        }
        result.sort(Comparator.comparingInt(PlayerProfile::level).
                reversed().
                thenComparing(PlayerProfile::nickname));
        return List.copyOf(result);
    }
}
