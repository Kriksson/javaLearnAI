package learning.task092;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    private final JdbcTemplate jdbcTemplate;
    public Controller(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/bank/payments/count")
    public ResponseEntity<BankPaymentsCountDTO> getBankPaymentsCount() {
        long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_requests", Long.class);
        return ResponseEntity.ok(new BankPaymentsCountDTO(count));
    }
}
