package learning.task082;


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
        Recruit recruit = recruitRepository.save(fromRequest);
        RecruitDTO recruitDTO = new RecruitDTO(recruit.getId(), recruit.getName(), recruit.getLevel());
        URI location = URI.create("/recruits/"+recruit.getId());
        return ResponseEntity.created(location).body(recruitDTO);
    }

    @GetMapping("/recruits/{id}")
    ResponseEntity<RecruitDTO> getRecruit(@PathVariable Long id) {
        Recruit recruit =  recruitRepository.findById(id).orElse(null);
        if (recruit == null) {
            return ResponseEntity.notFound().build();
        } else {
            RecruitDTO recruitDTO = new RecruitDTO(recruit.getId(), recruit.getName(), recruit.getLevel());
            return ResponseEntity.ok(recruitDTO);
        }
    }

}
