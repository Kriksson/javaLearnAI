package learning.task080;


import jakarta.persistence.*;

@Entity
@Table(name = "guild_keepers")
public class GuildKeeper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level;

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    protected GuildKeeper() {}

    protected GuildKeeper(String name, Integer level, Guild guild) {
        this.name = name;
        this.level = level;
        this.guild = guild;
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

    public Guild getGuild() {
        return guild;
    }
}
