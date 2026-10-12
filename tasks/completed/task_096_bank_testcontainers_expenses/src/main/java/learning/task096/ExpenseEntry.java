package learning.task096;

import jakarta.persistence.*;

@Entity
@Table(name = "expense_entries")
public class ExpenseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    protected ExpenseEntry() {
    }

    public ExpenseEntry(String description, Long amountMinor) {
        this.description = description;
        this.amountMinor = amountMinor;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }
}
