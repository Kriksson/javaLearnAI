package learning.task098;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ExpenseEntryRepository extends JpaRepository<ExpenseEntry, Long> {
    Long countByAmountMinorGreaterThanEqual(Long minimumAmount);
}
