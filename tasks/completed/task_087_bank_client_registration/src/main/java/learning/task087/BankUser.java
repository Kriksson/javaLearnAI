package learning.task087;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_users")
public class BankUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 40)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 40)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    protected BankUser() {}

    public BankUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }
}
