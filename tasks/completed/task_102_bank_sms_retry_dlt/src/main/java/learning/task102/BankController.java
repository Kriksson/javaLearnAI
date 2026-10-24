package learning.task102;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import learning.task102.DTO.KafkaMessageDTO;
import learning.task102.DTO.MessageBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
public class BankController {

    private final BankService bankService;
    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @PostMapping("/api/sms-requests")
    public ResponseEntity<Void> post(@Valid @RequestBody MessageBody messageBody)
            throws ExecutionException, JsonProcessingException, InterruptedException, TimeoutException {
        bankService.sendMessage(messageBody);
        return ResponseEntity.status(202).build();
    }

    @GetMapping("/api/sms-requests")
    public ResponseEntity<List<KafkaMessageDTO>> get(@RequestParam(value = "clientId", required = true) Long clientId) {
        if (clientId < 1) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(bankService.getMessagesById(clientId));
    }
}
