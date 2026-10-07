package learning.task079;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class GuildService {

    private final GuildKeeperRepository guildKeeperRepository;
    private final GuildRepository guildRepository;

    public GuildService(GuildKeeperRepository guildKeeperRepository, GuildRepository guildRepository) {
        this.guildKeeperRepository = guildKeeperRepository;
        this.guildRepository = guildRepository;
    }

    @Transactional
    public void changeKeeperGuild(Long keeperId, Long guildId) {
        if (guildKeeperRepository.existsById(keeperId)) {
            if (guildRepository.existsById(guildId)) {
                GuildKeeper guildKeeper = guildKeeperRepository.findById(keeperId)
                        .orElseThrow(IllegalArgumentException::new);
                Guild guild = guildRepository.findById(guildId).orElseThrow(IllegalArgumentException::new);
                guildKeeper.changeGuild(guild);
            } else {
                throw new IllegalArgumentException("Несуществующий Guild");
            }
        } else {
            throw new IllegalArgumentException("Несуществующий Keeper");
        }
    }

}
