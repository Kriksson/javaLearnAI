package learning.task083;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class Controller {

    private final RecruitRepository recruitRepository;
    public Controller(RecruitRepository recruitRepository) {
        this.recruitRepository = recruitRepository;
    }

    @PostMapping("/recruits")
    ResponseEntity<RecruitDTO> createRecruit(@Valid @RequestBody RecruitBody body) {
        Recruit fromRequest = new Recruit(body.name(), body.level());
        Recruit fromDB = recruitRepository.save(fromRequest);
        URI location = URI.create("/recruits/" + fromDB.getId());
        RecruitDTO recruitDTO = new RecruitDTO(fromDB.getId(), fromDB.getName(), fromDB.getLevel());
        return ResponseEntity.created(location).body(recruitDTO);
    }

    @GetMapping("/recruits/{id}")
    ResponseEntity<RecruitDTO> getRecruit(@PathVariable Long id) {
        Recruit fromDB = recruitRepository.findById(id).orElse(null);
        if (fromDB == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(new RecruitDTO(fromDB.getId(), fromDB.getName(), fromDB.getLevel()));
        }
    }
}
