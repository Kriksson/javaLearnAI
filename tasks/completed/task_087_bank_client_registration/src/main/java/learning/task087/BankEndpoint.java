package learning.task087;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class BankEndpoint {

    private final BankUserRepository bankUserRepository;
    private final PasswordEncoder passwordEncoder;
    public BankEndpoint(BankUserRepository bankUserRepository,  PasswordEncoder passwordEncoder) {
        this.bankUserRepository = bankUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/bank/info")
    ResponseEntity<BankInfoDTO> getBankInfo() {
        return ResponseEntity.ok(new BankInfoDTO("Practice Bank"));
    }

    @GetMapping("/bank/csrf")
    ResponseEntity<CsrfDTO> getCsrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfDTO(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/bank/register")
    ResponseEntity<BankUserWithIDDTO> registerClient(@Valid @RequestBody BankUserBody userBody) {
        if (bankUserRepository.existsByUsername(userBody.username())) {
            return ResponseEntity.status(409).build();
        } else {
            String password_hash =  passwordEncoder.encode(userBody.password());
            BankUser fromRequestUser = new BankUser(userBody.username(), password_hash, "CLIENT");
            BankUser fromDataBaseUser = bankUserRepository.save(fromRequestUser);
            BankUserWithIDDTO bankUserWithIDDTO = new BankUserWithIDDTO(fromDataBaseUser.getId(), fromDataBaseUser.getUsername(),
                    fromDataBaseUser.getRole());
            return ResponseEntity.status(201).body(bankUserWithIDDTO);
        }
    }

    @GetMapping("/bank/me")
    ResponseEntity<BankUserWithoutIdDTO> getCurrentUser(Principal principal) {
        BankUser bankUser = bankUserRepository.findByUsername(principal.getName());
        if (bankUser == null) {
            return ResponseEntity.status(404).build();
        } else {
            BankUserWithoutIdDTO bankUserWithoutIDDTO =  new BankUserWithoutIdDTO(bankUser.getUsername(), bankUser.getRole());
            return ResponseEntity.ok(bankUserWithoutIDDTO);
        }
    }

    @GetMapping("/bank/client-count")
    ResponseEntity<BankClientCountDTO> getClientCount() {
        long count  = bankUserRepository.countBankUserByRole("CLIENT");
        return  ResponseEntity.ok(new BankClientCountDTO(count));
    }

}
