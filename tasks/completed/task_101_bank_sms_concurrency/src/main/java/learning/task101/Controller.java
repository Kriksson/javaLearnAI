package learning.task101;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
public class Controller {
    private final Service service;
    public Controller(Service service) {
        this.service = service;
    }

    @PostMapping("/api/sms-requests")
    public ResponseEntity<Void> post(@Valid @RequestBody SMSRequestBody requestBody)
            throws ExecutionException, JsonProcessingException, InterruptedException, TimeoutException {
        service.sendMessage(requestBody);
        return ResponseEntity.status(202).build();
    }

    @GetMapping("/api/sms-requests")
    public ResponseEntity<List<KafkaMessageDTO>> get(@RequestParam(required = true, value = "clientId") Long clientId) {
        if (clientId < 1) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.getByClientId(clientId));
    }
}
