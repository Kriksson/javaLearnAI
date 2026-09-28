package learning.task060;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/players/{id}")
    public ResponseEntity<PlayerView> get(@PathVariable("id") int id) {
        if (id < 1) return ResponseEntity.badRequest().build();
        PlayerView result = playerService.find(id);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/players/{id}/points")
    public ResponseEntity<PlayerView> award(@PathVariable("id") int id, @RequestBody PointAwardRequest request) {
        // TODO: проверь ID, наличие игрока и очки; вызови сервис и верни результат.
        if (id < 1) return ResponseEntity.badRequest().build();
        PlayerView result = playerService.find(id);
        if (result == null) return ResponseEntity.notFound().build();
        if (request == null || request.points() == null)  return ResponseEntity.badRequest().build();
        int points = request.points();
        if (points < 1 || points > 20) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(playerService.award(id, request.points()));
    }
}
