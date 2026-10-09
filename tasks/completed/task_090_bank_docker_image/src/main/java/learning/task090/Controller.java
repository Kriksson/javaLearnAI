package learning.task090;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    private final String branch;

    public Controller(@Value("${bank.branch}") String branch) {
        this.branch = branch;
    }


    @GetMapping("/bank/info")
    ResponseEntity<BankInfoDTO> getBankInfo() {
        return ResponseEntity.ok(new BankInfoDTO("Practice Bank", branch));
    }
}
