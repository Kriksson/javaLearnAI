package learning.task093;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    private final PaymentRequestRepository paymentRequestRepository;
    public Controller(PaymentRequestRepository paymentRequestRepository) {
        this.paymentRequestRepository = paymentRequestRepository;
    }

    @GetMapping("/bank/payments/count")
    public ResponseEntity<PaymentsCountDTO> getPaymentsCount() {
        long count = paymentRequestRepository.count();
        return ResponseEntity.ok(new PaymentsCountDTO(count));
    }
}
