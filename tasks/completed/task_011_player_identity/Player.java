package learning.task011;

import java.util.Objects;

public class Player {
    private final String displayName;
    private final String id;

    public Player(String id, String displayName) {
        id = id.trim();
        displayName = displayName.trim();
        if (id.isEmpty() || displayName.isEmpty()) throw new IllegalArgumentException();
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Player player)) return false;
        return id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", id, displayName);
    }
}
