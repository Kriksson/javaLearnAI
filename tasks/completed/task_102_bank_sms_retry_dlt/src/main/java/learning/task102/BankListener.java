package learning.task102;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BankListener {

    private final BankService bankService;
    public BankListener(BankService bankService) {
        this.bankService = bankService;
    }

    @KafkaListener(topics = "bank.sms.v1", groupId = "bank-sms-senders-v1", concurrency = "3")
    public void listen(ConsumerRecord<String, String> record) throws IOException, InterruptedException {
        bankService.sendPost(record);
    }
}
