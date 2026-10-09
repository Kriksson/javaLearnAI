package learning.task088;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankUserRepository extends JpaRepository<BankUser, Long> {
    BankUser findByUsername(String username);
    Long countBankUsersByRole(String role);
}
