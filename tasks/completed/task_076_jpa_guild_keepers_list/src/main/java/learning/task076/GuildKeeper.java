package learning.task076;


import jakarta.persistence.*;

@Entity
@Table(name = "guild_keepers")
public class GuildKeeper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length =  40)
    private String name;

    @Column(name = "level", nullable = false)
    private int level;

    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public Guild getGuild() {
        return guild;
    }
}
