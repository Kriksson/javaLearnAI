package learning.task088;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class BankEndpoint {

    private final BankService bankService;
    public BankEndpoint(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/bank/info")
    ResponseEntity<BankInfoDTO> getBankInfo() {
        return ResponseEntity.ok(new BankInfoDTO("Practice Bank"));
    }

    @GetMapping("/bank/me")
    ResponseEntity<BankUserDTO> getBankUserInfo(Principal principal) {
        try {
            BankUserDTO bankUserDTO = bankService.getBankUserInfo(principal);
            return ResponseEntity.ok(bankUserDTO);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/bank/client-count")
    ResponseEntity<BankClientCountDTO> getBankClientCount() {
        long count = bankService.getBankUserCount();
        return ResponseEntity.ok(new BankClientCountDTO(count));
    }

}
