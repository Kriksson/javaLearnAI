package learning.task072;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuildController {

    private final GuildService guildService;
    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @PostMapping("/guild/coin-transfers")
    public ResponseEntity transferCoin(@Valid @RequestBody ChestTransactionBody body) {
        if (body.fromId() == body.toId()) return ResponseEntity.badRequest().build();
        try {
            guildService.transactionCoin(body.fromId(), body.toId());
            return ResponseEntity.noContent().build();
        } catch (SourceHasntCoinsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (ReceiverNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
