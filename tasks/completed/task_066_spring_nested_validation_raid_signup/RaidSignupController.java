package learning.task066;

import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class RaidSignupController {

    RaidService raidService;
    public RaidSignupController(RaidService raidService) {
        this.raidService = raidService;
    }

    @PostMapping("/guild/raid-signups") // Получаем имя пользователя и персонажа - регистрируем и возвращаем локацию
    ResponseEntity<RaidBody> signUpRaid(@Valid @RequestBody UserBody userBody) {
        try {
            long id = raidService.addHeroToRaid(userBody);
            URI location = URI.create("/guild/raid-signups/" + id);
            RaidBody raidBody = new RaidBody(id, userBody.playerName(), userBody.hero());
            return ResponseEntity.created(location).body(raidBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/guild/raid-signups/{id}") // Возвращаем заявку по ID
    ResponseEntity<RaidBody> getRaidSignup(@PathVariable("id") long id) {
        try {
            UserBody raidInfo = raidService.getRaidInfo(id);
            RaidBody raidBody = new RaidBody(id, raidInfo.playerName(), raidInfo.hero());
            return ResponseEntity.ok(raidBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
