package learning.task097;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    private final ExpenseEntryRepository expenseEntryRepository;
    public Controller(ExpenseEntryRepository expenseEntryRepository) {
        this.expenseEntryRepository = expenseEntryRepository;
    }

    @GetMapping("/bank/expenses/count")
    public ResponseEntity<CountDTO> getCount() {
        long count = expenseEntryRepository.count();
        return ResponseEntity.ok(new CountDTO(count));
    }

}
