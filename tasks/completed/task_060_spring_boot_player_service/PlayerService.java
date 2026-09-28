package learning.task060;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PlayerService {
    private final Map<Integer, PlayerView> players = new HashMap<>(Map.of(
            1, new PlayerView(1, "Мира", 10),
            2, new PlayerView(2, "Тор", 50)
    ));

    public PlayerView find(int id) {
        if (players.containsKey(id)) {
            return players.get(id);
        }
        return null;
    }

    public PlayerView award(int id, int points) {
        if (players.containsKey(id)) {
            PlayerView playerView = players.get(id);
            PlayerView newPlayerView = new PlayerView(id, playerView.name(), Math.max(0, playerView.score() + points));
            players.put(id, newPlayerView);
            return newPlayerView;
        }
        return null;
    }
}
