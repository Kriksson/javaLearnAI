package learning.task076;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuildKeeperRepository extends JpaRepository<GuildKeeper, Long> {
    List<GuildKeeper> findByGuild_IdOrderByIdAsc(long id);
}
