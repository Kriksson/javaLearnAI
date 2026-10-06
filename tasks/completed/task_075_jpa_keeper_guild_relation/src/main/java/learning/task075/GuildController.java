package learning.task075;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class GuildController {

    GuildKeeperRepository  guildKeeperRepository;
    GuildRepository guildRepository;
    public GuildController(GuildKeeperRepository guildKeeperRepository, GuildRepository guildRepository) {
        this.guildKeeperRepository = guildKeeperRepository;
        this.guildRepository = guildRepository;
    }

    @GetMapping("/guild/keepers/{id}")
    ResponseEntity<GuildKeeperResponse> getGuild(@PathVariable("id") Long id) {
        GuildKeeper guildKeeper =  guildKeeperRepository.findById(id).orElse(null);
        if (guildKeeper == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<Guild> g = guildKeeper.getGuild();
        if (g.isPresent()) {
            Guild guild = g.get();
            GuildKeeperResponse answer = new GuildKeeperResponse(id, guildKeeper.getName(), guildKeeper.getLevel(), guild);
            return ResponseEntity.ok(answer);
        } else {
            return ResponseEntity.notFound().build();
        }

    }
}
