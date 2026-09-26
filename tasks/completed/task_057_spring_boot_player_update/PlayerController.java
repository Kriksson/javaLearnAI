package learning.task057;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PlayerController {
    private final Map<Integer, PlayerView> players = new HashMap<>(Map.of(
            1, new PlayerView(1, "Мира", 10),
            2, new PlayerView(2, "Тор", 20)
    ));

    @GetMapping("/players/{id}")
    public ResponseEntity<PlayerView> get(@PathVariable("id") int id) {
        if (id < 1) return ResponseEntity.badRequest().build();
        if (players.containsKey(id)) {
            return ResponseEntity.ok(players.get(id));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/players/{id}")
    public ResponseEntity<PlayerView> update(@PathVariable("id") int id, @RequestBody PlayerUpdateRequest request) {
        if (id < 1) return ResponseEntity.badRequest().build();
        if (players.containsKey(id)) {
            if (request == null || request.name() == null || request.level() == null) return ResponseEntity.badRequest().build();
            String updateName = request.name().trim();
            int updateLevel = request.level();
            if (updateName.isEmpty() || updateName.length() > 20 || updateLevel < 1 || updateLevel > 100)
                return ResponseEntity.badRequest().build();

            players.put(id, new PlayerView(id, updateName, updateLevel));
            return ResponseEntity.ok(players.get(id));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
