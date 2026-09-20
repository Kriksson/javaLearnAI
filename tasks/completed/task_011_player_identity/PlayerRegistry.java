package learning.task011;

import java.util.HashSet;
import java.util.Set;

public class PlayerRegistry {

    private final Set<Player> players = new HashSet<>();

    public boolean register(Player player) {
        if (player == null) throw new IllegalArgumentException();
        if (contains(player)) return false;
        players.add(player);
        return true;
    }

    public int size() {
        return players.size();
    }

    public boolean contains(Player player) {
        if (player == null) throw new IllegalArgumentException();
        return  players.contains(player);
    }
}
