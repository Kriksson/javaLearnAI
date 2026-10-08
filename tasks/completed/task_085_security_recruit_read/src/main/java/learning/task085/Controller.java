package learning.task085;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    private final RecruitRepository recruitRepository;
    public Controller(RecruitRepository recruitRepository) {
        this.recruitRepository = recruitRepository;
    }

    @GetMapping("/guild/info")
    ResponseEntity<GuildDTO> getGuildInfo() {
        return ResponseEntity.ok(new GuildDTO("Training Guild"));
    }

    @GetMapping("/recruits/{id}")
    ResponseEntity<RecruitDTO> getRecruitById(@PathVariable("id") Long id) {
        if (id < 0) return ResponseEntity.badRequest().build();
        Recruit fromRequest = recruitRepository.findById(id).orElse(null);
        if (fromRequest == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new RecruitDTO(id, fromRequest.getName(), fromRequest.getLevel(),
                fromRequest.getCoins()));
    }

    @GetMapping("/guild/recruit-count")
    ResponseEntity<GuildCountDTO> getRecruitCount() {
        return ResponseEntity.ok(new GuildCountDTO(recruitRepository.count()));
    }
}
