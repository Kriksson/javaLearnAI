package learning.task072;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuildService {

    private final GuildRepository guildRepository;
    public GuildService(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    @Transactional
    public void transactionCoin(long fromId, long toId) {
        if (guildRepository.take(fromId) != 1) {
            throw new SourceHasntCoinsException("У источника нет монет");
        }
        if (guildRepository.put(toId) != 1) {
            throw new ReceiverNotFoundException("Получатель не найден!");
        }
    }
}
