package learning.task061;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MatchController {
    @PostMapping("/matches/preview")
    public ResponseEntity<MatchPreview> preview(@Valid @RequestBody MatchRequest request) {
        int level = request.level();
        boolean ranked = false;
        if (level >= 50) ranked = true;
        MatchPreview result = new MatchPreview(request.playerName(), request.opponentId(),  ranked);
        return ResponseEntity.ok(result);
    }
}
