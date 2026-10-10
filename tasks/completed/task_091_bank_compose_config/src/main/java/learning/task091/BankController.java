package learning.task091;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BankController {

    private String region;

    public BankController(@Value("${bank.region}") String region) {
        this.region = region;
    }


    @GetMapping("/bank/status")
    ResponseEntity<BankStatusDTO> getBankStatus() {
        return ResponseEntity.ok(new BankStatusDTO("Transfer API", region));
    }
}
