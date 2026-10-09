package learning.task087;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankUserRepository extends JpaRepository<BankUser, Long> {

    BankUser findByUsername(String username);

    boolean existsByUsername(String username);

    Long countBankUserByRole(String role);

}
