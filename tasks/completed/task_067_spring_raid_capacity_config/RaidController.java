package learning.task067;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RaidController {

    private final RaidService raidService;
    private final RaidSettings raidSettings;
    public RaidController(RaidSettings raidSettings, RaidService raidService) {
        this.raidSettings = raidSettings;
        this.raidService = raidService;
    }

    @PostMapping("/guild/raid-nights/{raidId}/participants")
    ResponseEntity<RaidBody> registerOnRaid(@Valid @Min(1) @PathVariable("raidId") long raidId,
                                            @Valid @RequestBody Player player) {
        try {
            String playerName = player.playerName();
            if (raidService.getReservations(raidId) < raidSettings.maxParticipantsPerRaid()) {
                raidService.addReservations(raidId);
                RaidBody raidBody = new RaidBody(raidService.getReservations(raidId), raidId, playerName);
                URI location = URI.create("/guild/raid-nights/" + raidId + "/participants/" + raidService.getReservations(raidId));
                return ResponseEntity.created(location).body(raidBody);
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
