package learning.task062;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlayerPreviewController {
    @PostMapping("/players/preview")
    public ResponseEntity<PlayerPreview> preview(@Valid @RequestBody PlayerRequest request) {
        return ResponseEntity.ok(new PlayerPreview(request.name(), request.level()));
    }
}
