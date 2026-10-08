package learning.task082;

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

    protected Recruit() {}

    public Recruit(String name, Integer level) {
        this.name = name;
        this.level = level;
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
}
