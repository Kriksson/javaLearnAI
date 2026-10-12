package learning.task096;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class Controller {

    private final ExpenseEntryRepository expenseEntryRepository;
    public Controller(ExpenseEntryRepository expenseEntryRepository) {
        this.expenseEntryRepository = expenseEntryRepository;
    }

    @PostMapping("/bank/expenses")
    public ResponseEntity<ExpenseEntryDTO> addExpense(@Valid @RequestBody ExpenseBody expenseBody) {
        ExpenseEntry fromRequest = new ExpenseEntry(expenseBody.description(), expenseBody.amountMinor());
        ExpenseEntry fromDB = expenseEntryRepository.save(fromRequest);
        URI location = URI.create("/bank/expenses/"+ fromDB.getId());
        ExpenseEntryDTO expenseEntryDTO = new ExpenseEntryDTO(fromDB.getId(), fromDB.getDescription(),
                fromDB.getAmountMinor());
        return ResponseEntity.created(location).body(expenseEntryDTO);
    }

    @GetMapping("/bank/expenses/{id}")
    public ResponseEntity<ExpenseEntryDTO> getExpenseById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) return ResponseEntity.badRequest().build();
        ExpenseEntry fromDB = expenseEntryRepository.findById(id).orElse(null);
        if (fromDB == null) return ResponseEntity.notFound().build();
        ExpenseEntryDTO expenseEntryDTO = new ExpenseEntryDTO(fromDB.getId(), fromDB.getDescription(),
                fromDB.getAmountMinor());
        return ResponseEntity.ok(expenseEntryDTO);
    }
}
