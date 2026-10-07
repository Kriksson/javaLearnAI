package learning.task080;

import java.util.List;

public record KeeperPageDTO(List<KeeperDTO> content, int page, int size, long totalElements, int totalPages) {
}
