package learning.task056;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ScorePreviewController {
    @PostMapping("/scores/preview")
    public ResponseEntity<ScorePreview> preview(@RequestBody ScorePreviewRequest request) {
        if (request == null || request.wins() == null || request.losses() == null
                || request.losses() > 100 || request.losses() < 0
                || request.wins() > 100 || request.wins() < 0 || (request.losses() == 0 && request.wins() == 0)) {
            return ResponseEntity.badRequest().build();
        }
        boolean bonus = false;
        Integer wins = request.wins();
        Integer losses = request.losses();
        int scores = Math.max(0, wins * 3 - losses);
        if (wins >= 5) {
            scores += 10;
            bonus = true;
        }
        return ResponseEntity.ok(new ScorePreview(scores, bonus));
    }
}
