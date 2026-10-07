package learning.task080;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuildKeeperRepository extends JpaRepository<GuildKeeper, Long> {
    Page<GuildKeeper> findByGuild_IdAndLevelGreaterThanEqualOrderByLevelDescIdAsc(Long guild_Id, Integer level, Pageable pageable);
}
