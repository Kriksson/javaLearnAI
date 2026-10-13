package learning.task098;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private final ExpenseEntryRepository expenseEntryRepository;
    public Controller(ExpenseEntryRepository expenseEntryRepository) {
        this.expenseEntryRepository = expenseEntryRepository;
    }

    @GetMapping("/bank/expenses/count")
    public ResponseEntity<CountDTO> getCount(@RequestParam(
            name = "minimumAmountMinor", required = false, defaultValue = "1") Long minimumAmountMinor) {
        if (minimumAmountMinor == null || minimumAmountMinor < 1) {
            log.warn("event=expense_count_rejected minimumAmountMinor={}", minimumAmountMinor);
            return ResponseEntity.badRequest().build();
        }
        long count = expenseEntryRepository.countByAmountMinorGreaterThanEqual(minimumAmountMinor);
        log.info("event=expense_count_read minimumAmountMinor={} count={}", minimumAmountMinor, count);
        return ResponseEntity.ok(new CountDTO(count));
    }
}
