package learning.task086;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankUserRepository extends JpaRepository<BankUser, Long> {
    BankUser findByUsername(String username);
    Long countByRole(String role);
}
