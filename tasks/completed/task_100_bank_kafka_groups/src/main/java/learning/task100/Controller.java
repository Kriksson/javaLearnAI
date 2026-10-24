package learning.task100;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import learning.task100.DTO.KafkaMessageDTO;
import learning.task100.DTO.MessageDTO;
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


    @PostMapping("/api/notifications")
    public ResponseEntity<Void> createNotify(@Valid @RequestBody MessageDTO messageDTO)
            throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        service.sendMessage(messageDTO);
        return ResponseEntity.status(202).build();
    }

    @GetMapping("/api/notifications/delivery")
    public ResponseEntity<List<KafkaMessageDTO>> getDeliveriesById(
            @RequestParam(value = "clientId", required = true) Long id) {
        if (id <= 0) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.getMessagesById(id));
    }

    @GetMapping("/api/notifications/audit")
    public ResponseEntity<List<KafkaMessageDTO>> getAudit(
            @RequestParam(value = "clientId", required = true) Long id) {
        if (id <= 0) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.getAuditsById(id));
    }
}
