package learning.task081;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuildController {

    private final GuildService guildService;

    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @GetMapping("/keepers")
    public ResponseEntity<KeeperPageDTO> getKeepersWithParams(@RequestParam("minLevel") int minLevel,
                                                                    @RequestParam("page") int page, @RequestParam("size") int size) {
        try {
            Page<GuildKeeper> guildKeeperPage = guildService.getKeepersWithParams(minLevel, page, size);
            Page<KeeperDTO> keeperDTOPage = guildKeeperPage
                    .map(e -> {
                        Guild guild = e.getGuild();
                        GuildDTO guildDTO = new GuildDTO(guild.getId(), guild.getName());
                        return new KeeperDTO(e.getId(), e.getName(), e.getLevel(), guildDTO);
                    });
            KeeperPageDTO keeperPageDTO = new KeeperPageDTO(keeperDTOPage.getContent(),
                    page, size, keeperDTOPage.getTotalElements(), keeperDTOPage.getTotalPages());
            return ResponseEntity.ok(keeperPageDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }


    }

}
