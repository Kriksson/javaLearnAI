package learning.task081;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildKeeperRepository extends JpaRepository<GuildKeeper, Long> {
    @EntityGraph(attributePaths = "guild")
    Page<GuildKeeper> findByLevelGreaterThanEqualOrderByLevelDescIdAsc(Integer level, Pageable pageable);
}
