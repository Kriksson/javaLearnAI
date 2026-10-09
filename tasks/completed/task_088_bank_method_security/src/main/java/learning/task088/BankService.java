package learning.task088;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class BankService {

    private final BankUserRepository bankUserRepository;

    public BankService(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    public BankUserDTO getBankUserInfo(Principal principal) {
        BankUser bankUser = bankUserRepository.findByUsername(principal.getName());
        if (bankUser == null) {
            throw new UsernameNotFoundException("Пользователь не найден");
        } else {
            return new BankUserDTO(bankUser.getUsername(), bankUser.getRole());
        }
    }

    @PreAuthorize("hasRole('AUDITOR')")
    public Long getBankUserCount() {
        return bankUserRepository.countBankUsersByRole("CLIENT");
    }

}
