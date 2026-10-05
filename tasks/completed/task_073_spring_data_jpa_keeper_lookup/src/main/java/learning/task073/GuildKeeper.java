package learning.task073;


import jakarta.persistence.*;

@Entity
@Table(name = "guild_keepers")
public class GuildKeeper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level;

    public GuildKeeper(Long id, String name, Integer level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }

    protected GuildKeeper() {

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
