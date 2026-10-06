package learning.task078;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GuildController {

    GuildKeeperRepository guildKeeperRepository;
    GuildRepository guildRepository;
    public GuildController(GuildKeeperRepository guildKeeperRepository, GuildRepository guildRepository) {
        this.guildKeeperRepository = guildKeeperRepository;
        this.guildRepository = guildRepository;
    }

    @GetMapping("/guilds/{id}/keepers")
    ResponseEntity<List<GuildKeeperBody>> getGuildKeepers(@PathVariable("id") long id,
                                                @RequestParam("minLevel") int minLevel) {
        if (minLevel < 0) return ResponseEntity.badRequest().build();
        if (guildRepository.existsById(id)) {
            List<GuildKeeper> guildKeeperList = guildKeeperRepository
                    .findByGuild_IdAndLevelGreaterThanEqualOrderByLevelDescIdAsc(id, minLevel);
            List<GuildKeeperBody> guildKeeperBodyList = guildKeeperList.stream()
                    .map(e -> new GuildKeeperBody(e.getId(), e.getName(), e.getLevel()))
                    .toList();
            return ResponseEntity.ok(guildKeeperBodyList);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
