package learning.task080;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuildController {

    private final GuildService guildService;
    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @GetMapping("/guilds/{guildId}/keepers")
    ResponseEntity<KeeperPageDTO> readKeeperWithParam(@PathVariable Long guildId,
                                                      @RequestParam("minLevel") int minLevel,
                                                      @RequestParam("page") int page,
                                                      @RequestParam("size") int size) {
        try {
            Page<GuildKeeper> pages = guildService.readPages(guildId, minLevel, page, size);
            Page<KeeperDTO> dtoPage = pages.map(e -> new KeeperDTO(e.getId(), e.getName(), e.getLevel()));
            KeeperPageDTO keeperPageDTO = new KeeperPageDTO(
                    dtoPage.getContent(), page, size,
                    dtoPage.getTotalElements(), dtoPage.getTotalPages());
            return ResponseEntity.ok(keeperPageDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (GuildNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }

}
