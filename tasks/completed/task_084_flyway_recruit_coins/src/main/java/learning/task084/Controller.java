package learning.task084;

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
        Recruit fromRequest = new Recruit(body.name(), body.level(), 0);
        Recruit fromDB = recruitRepository.save(fromRequest);
        Long id = fromDB.getId();
        URI location = URI.create("/recruits/" + id);
        RecruitDTO recruitDTO = new RecruitDTO(id, fromDB.getName(),  fromDB.getLevel(), fromDB.getCoins());
        return ResponseEntity.created(location).body(recruitDTO);
    }

    @GetMapping("/recruits/{id}")
    ResponseEntity<RecruitDTO> getRecruitById(@PathVariable Long id) {
        Recruit fromDB = recruitRepository.findById(id).orElse(null);
        if (fromDB == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(new RecruitDTO(id, fromDB.getName(),  fromDB.getLevel(), fromDB.getCoins()));
        }
    }
}
