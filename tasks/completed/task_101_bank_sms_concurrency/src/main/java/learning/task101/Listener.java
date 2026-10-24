package learning.task101;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
public class Listener {

    private final Service service;
    public Listener(Service service) {
        this.service = service;
    }

    @KafkaListener(topics = "bank.sms.v1", concurrency = "3", groupId = "bank-sms-senders-v1")
    public void listen(ConsumerRecord<String, String> record) throws URISyntaxException, IOException, InterruptedException {
        service.sendPostGateway(record);
    }
}
