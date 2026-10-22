package learning.task099;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BankBrokerReceiver {

    private final BankNotifyService bankNotifyService;
    public BankBrokerReceiver(BankNotifyService bankNotifyService) {
        this.bankNotifyService = bankNotifyService;
    }

    @KafkaListener(topics = "bank.notifications.v1")
    public void listen(String message) throws JsonProcessingException {
        bankNotifyService.addQueue(message);
    }
}
