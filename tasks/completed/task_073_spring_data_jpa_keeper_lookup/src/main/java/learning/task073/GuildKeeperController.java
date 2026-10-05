package learning.task073;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class GuildKeeperController {

    GuildKeeperRepository guildKeeperRepository;
    public GuildKeeperController(GuildKeeperRepository guildKeeperRepository) {
        this.guildKeeperRepository = guildKeeperRepository;
    }

    @GetMapping("/guild/keepers/{id}")
    ResponseEntity<GuildKeeper> getGuildKeeper(@PathVariable("id") long id) {
        if (id < 0) return ResponseEntity.badRequest().build();
        Optional<GuildKeeper> guildKeeper = guildKeeperRepository.findById(id);
        return  guildKeeper.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
