package learning.task085;


import jakarta.persistence.*;

@Entity
@Table(name = "recruits")
public class Recruit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "coins", nullable = false)
    private Integer coins;

    protected Recruit() {}

    public Recruit(String name, Integer level, Integer coins) {
        this.name = name;
        this.level = level;
        this.coins = coins;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getLevel() {
        return level;
    }

    public Integer getCoins() {
        return coins;
    }
}
