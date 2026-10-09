package learning.task089;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BankService {

    private final BankUserRepository bankUserRepository;
    public BankService(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @PreAuthorize("#owner == authentication.name or hasRole('AUDITOR')" )
    public BankUserDTO getBankUser(@P("owner") String username) {
        BankUser bankUser = bankUserRepository.findByUsername(username);
        if (bankUser == null) {
            throw new ProfileNotFoundException("User not found");
        } else {
            return new BankUserDTO(bankUser.getUsername(), bankUser.getRole());
        }
    }

}
