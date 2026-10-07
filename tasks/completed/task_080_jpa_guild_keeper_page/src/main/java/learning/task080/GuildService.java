package learning.task080;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildService {
    GuildKeeperRepository guildKeeperRepository;
    GuildRepository  guildRepository;

    public GuildService(GuildKeeperRepository guildKeeperRepository, GuildRepository guildRepository) {
        this.guildKeeperRepository = guildKeeperRepository;
        this.guildRepository = guildRepository;
    }

    Page<GuildKeeper> readPages(Long guildId, Integer minLevel, int page, int size) {
        if (minLevel == null || minLevel < 0 || page < 0 || page > 1000 || size < 1 || size > 5) {
            throw new IllegalArgumentException();
        }
        if (!guildRepository.existsById(guildId)) throw new GuildNotFoundException("Гильдия не найдена");
        var pageRequest = PageRequest.of(page, size);
        return guildKeeperRepository
                .findByGuild_IdAndLevelGreaterThanEqualOrderByLevelDescIdAsc(guildId, minLevel, pageRequest);
    }
}
