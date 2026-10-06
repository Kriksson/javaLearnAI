package learning.task078;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuildKeeperRepository extends JpaRepository<GuildKeeper, Long> {
    List<GuildKeeper> findByGuild_IdAndLevelGreaterThanEqualOrderByLevelDescIdAsc(Long guildId, int level);
}
