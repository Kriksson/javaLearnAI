package learning.task068;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class GuildRecruitsController {

    private final GuildRecruitsRepository guildRecruitsRepository;
    public GuildRecruitsController(GuildRecruitsRepository guildRecruitsRepository) {
        this.guildRecruitsRepository = guildRecruitsRepository;
    }

    @GetMapping("/guild/recruits/{id}")
    ResponseEntity<Recruit> getRecruitById(@PathVariable("id") long id) {
        Optional<Recruit> r = guildRecruitsRepository.getRecruitById(id);
        return r.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
