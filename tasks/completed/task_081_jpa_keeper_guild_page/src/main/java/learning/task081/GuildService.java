package learning.task081;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class GuildService {

    private final GuildKeeperRepository guildKeeperRepository;
    private final GuildRepository guildRepository;

    public GuildService(GuildKeeperRepository guildKeeperRepository, GuildRepository guildRepository) {
        this.guildKeeperRepository = guildKeeperRepository;
        this.guildRepository = guildRepository;
    }

    public Page<GuildKeeper> getKeepersWithParams(int minLevel, int page, int size) {
        if (minLevel < 0 || page < 0 || page > 1000 || size < 0 || size > 5) throw new IllegalArgumentException();
        var request = PageRequest.of(page, size);
        return guildKeeperRepository.findByLevelGreaterThanEqualOrderByLevelDescIdAsc(minLevel, request);
    }

}
