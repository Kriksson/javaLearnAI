package learning.task066;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class RaidRepository {
    private long nextId = 1;
    private final Map<Long, UserBody> raidHeroes = new HashMap<>();

    public long addRaid(UserBody userBody) {
        if (userBody == null) throw new IllegalArgumentException("user body can't be null");
        raidHeroes.put(nextId, userBody);
        return nextId++;
    }

    public void removeRaid(long id) {
        if (id < 1 || id > raidHeroes.size()) throw new IllegalArgumentException("id out of range");
        raidHeroes.remove(id);
    }

    public Optional<UserBody> getRaid(long id) {
        if (id < 1 || id > raidHeroes.size()) throw new IllegalArgumentException("id out of range");
        return Optional.of(raidHeroes.get(id));
    }

    public Map<Long, UserBody> getRaidHeroes() {
        return Map.copyOf(raidHeroes);
    }
}
