package learning.task077;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GuildController {

    GuildKeeperRepository repository;
    GuildRepository guildRepository;
    public GuildController(GuildKeeperRepository repository,  GuildRepository guildRepository) {
        this.repository = repository;
        this.guildRepository = guildRepository;
    }

    @GetMapping("/guilds/{id}/keepers")
    ResponseEntity<List<GuildKeeperBody>> getGuildKeepers(@PathVariable("id") long id,
                                                      @RequestParam("name") String name,
                                                      @RequestParam("level") int level) {
        if (name.isBlank() ||  level < 0 || name.length() > 40) return ResponseEntity.badRequest().build();
        if (guildRepository.existsById(id)) {
            List<GuildKeeper> guildKeeperList = repository.findByGuild_IdAndNameAndLevelOrderByIdAsc(id, name, level);
            List<GuildKeeperBody> guildKeeperBodyList = guildKeeperList.stream()
                    .map(e -> new GuildKeeperBody(e.getId(), e.getName(), e.getLevel()))
                    .toList();
            return ResponseEntity.ok(guildKeeperBodyList);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
