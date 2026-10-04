package learning.task071;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuildController {
    private final GuildRepository guildRepository;
    public GuildController(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    @PutMapping("/guild/recruits/{id}")
    ResponseEntity<Boolean> setLevel(@Valid @RequestBody RecruitBody recruitBody, @PathVariable("id") long id) {
        if (guildRepository.setLevel(recruitBody, id) == 1) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
