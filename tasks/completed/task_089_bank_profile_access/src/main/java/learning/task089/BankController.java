package learning.task089;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankController {

    private final BankService bankService;
    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/bank/info")
    ResponseEntity<BankInfoDTO> getBankInfo() {
        return ResponseEntity.ok(new BankInfoDTO("Practice Bank"));
    }

    @GetMapping("/bank/users/{username}")
    ResponseEntity<BankUserDTO> getBankUser(@PathVariable String username) {
        try {
            return ResponseEntity.ok(bankService.getBankUser(username));
        } catch (ProfileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
