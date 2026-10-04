package learning.task070;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
public class RecruitController {

    RecruitRepository recruitRepository;
    public RecruitController(RecruitRepository recruitRepository) {
        this.recruitRepository = recruitRepository;
    }

    @PostMapping("/guild/recruits")
    ResponseEntity<RecruitBody> addRecruit(@Valid @RequestBody Recruit recruit) {
        try {
            long id = recruitRepository.add(recruit);
            URI location = URI.create("/guild/recruits/" + id);
            RecruitBody recruitBody = new RecruitBody(id, recruit.name(), recruit.level());
            return ResponseEntity.created(location).body(recruitBody);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/guild/recruits/{id}")
    ResponseEntity<RecruitBody> getRecruit(@PathVariable("id") long id) {
        Optional<Recruit> r = recruitRepository.findById(id);
        return r.map(recruit -> ResponseEntity.ok(new RecruitBody(id, recruit.name(), recruit.level())))
                .orElse(ResponseEntity.notFound().build());
    }

}
