package learning.task058;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
public class PlayerSearchController {
    private static final List<PlayerView> PLAYERS = List.of(
            new PlayerView(3, "Мирон", 30),
            new PlayerView(1, "Мира", 10),
            new PlayerView(5, "Тори", 100),
            new PlayerView(2, "Тор", 20),
            new PlayerView(4, "Ари", 1)
    );

    @GetMapping("/players")
    public ResponseEntity<List<PlayerView>> search(
            @RequestParam(value = "minLevel", required = false) Integer minLevel,
            @RequestParam(value = "name", required = false) String name) {
        if (minLevel != null && (minLevel < 1 || minLevel > 100)) return ResponseEntity.badRequest().build();
        String searchName = name == null ? null : name.trim().toLowerCase(Locale.ROOT);

        if (searchName != null && (searchName.isEmpty() || searchName.length() > 20))
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(PLAYERS.stream()
                .filter(p -> minLevel == null || p.level() >= minLevel)
                .filter(p -> searchName == null || p.name().toLowerCase(Locale.ROOT).contains(searchName))
                .sorted(Comparator.comparingInt(PlayerView::id))
                .toList());
    }
}
