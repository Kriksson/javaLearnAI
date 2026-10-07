package learning.task079;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuildController {

    private final GuildService guildService;
    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @PutMapping("/keepers/{keeperId}/guild")
    ResponseEntity<?> changeKeeperGuild(@PathVariable("keeperId") Long keeperId, @RequestBody GuildBody guildbody) {
        if (guildbody == null || guildbody.guildId() == null || guildbody.guildId() < 1) return ResponseEntity.badRequest().build();
        try {
            guildService.changeKeeperGuild(keeperId, guildbody.guildId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
