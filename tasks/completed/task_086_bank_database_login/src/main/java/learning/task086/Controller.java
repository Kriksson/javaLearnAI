package learning.task086;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class Controller {

    private final BankUserRepository bankUserRepository;
    public Controller(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @GetMapping("/bank/info")
    ResponseEntity<BankInfoDTO> getBankInfo() {
        return ResponseEntity.ok(new BankInfoDTO("Practice Bank"));
    }

    @GetMapping("/bank/me")
    ResponseEntity<BankUserDTO> getBankUser(Principal principal) {
        BankUser bankUser = bankUserRepository.findByUsername(principal.getName());
        if (bankUser == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(new BankUserDTO(bankUser.getUsername(), bankUser.getRole()));
        }
    }

    @GetMapping("/bank/client-count")
    ResponseEntity<ClientCountDTO> getClientCount() {
        Long count = bankUserRepository.countByRole("CLIENT");
        return ResponseEntity.ok(new ClientCountDTO(count));
    }
}
