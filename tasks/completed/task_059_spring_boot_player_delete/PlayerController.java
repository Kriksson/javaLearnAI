package learning.task059;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
public class PlayerController {
    private final Map<Integer, PlayerView> players = new HashMap<>(Map.of(
            1, new PlayerView(1, "Мира", 10),
            2, new PlayerView(2, "Тор", 20),
            3, new PlayerView(3, "Ари", 30)
    ));
    private final Set<Integer> activeMatches = Set.of(2);

    @GetMapping("/players/{id}")
    public ResponseEntity<PlayerView> get(@PathVariable("id") int id) {
        if (id <= 0) return ResponseEntity.badRequest().build();
        PlayerView player = players.get(id);
        if (player == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(player);
    }

    @DeleteMapping("/players/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") int id) {
        if (id < 1)  return ResponseEntity.badRequest().build();
        if (!players.containsKey(id)) {
            return ResponseEntity.notFound().build();
        } else {
            if  (activeMatches.contains(id)) {
                return ResponseEntity.status(409).build();
            } else {
                players.remove(id);
                return ResponseEntity.noContent().build();
            }
        }
    }
}
