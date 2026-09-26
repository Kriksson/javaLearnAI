package learning.task055;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PlayerController {
    private final Map<Integer, String> playerNames = Map.of(
            1, "Alice",
            7, "Кот \"Рыцарь\"");
    @GetMapping("/players/{id}")
    public ResponseEntity<PlayerView> find(@PathVariable("id") int id) {
        if (id > 0) {
            if (playerNames.containsKey(id)) {
                return ResponseEntity.ok(new PlayerView(id, playerNames.get(id)));
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
