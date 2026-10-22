package learning.task099;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
public class BankNotifyController {

    private final BankNotifyService bankNotifyService;
    public BankNotifyController(BankNotifyService bankNotifyService) {
        this.bankNotifyService = bankNotifyService;
    }

    @PostMapping("/api/notifications")
    public ResponseEntity<Void> notify(@Valid @RequestBody NotificationRequest body)
            throws ExecutionException, JsonProcessingException, InterruptedException, TimeoutException {
        bankNotifyService.publish(body);
        return ResponseEntity.status(202).build();
    }

    @GetMapping("/api/notifications")
    public ResponseEntity<List<NotificationRequest>> getNotifications(
            @RequestParam(value = "clientId", required = true) Long clientId) {
        if (clientId == null || clientId < 1) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(bankNotifyService.getQueueById(clientId));
    }
}
