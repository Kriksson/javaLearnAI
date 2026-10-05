package learning.task074;

import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class GuildKeeperController {

    GuildKeepersRepository guildKeepersRepository;
    public GuildKeeperController(GuildKeepersRepository guildKeepersRepository) {
        this.guildKeepersRepository = guildKeepersRepository;
    }

    @PostMapping("/guild/keepers")
    ResponseEntity<GuildKeeper> createKeeper(@RequestBody KeeperBody body) {
        GuildKeeper guildKeeper = new GuildKeeper(body.name(), body.level());
        GuildKeeper saved =  guildKeepersRepository.save(guildKeeper);
        URI loc = URI.create("/guild/keepers/" + saved.getId());
        return ResponseEntity.created(loc).body(guildKeeper);
    }

}
