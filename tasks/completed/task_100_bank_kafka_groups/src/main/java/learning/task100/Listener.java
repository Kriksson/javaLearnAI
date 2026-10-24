package learning.task100;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import learning.task100.DTO.KafkaMessageDTO;
import learning.task100.DTO.MessageDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Listener {

    ObjectMapper objectMapper = new ObjectMapper();
    private final Service service;
    public Listener(Service service) {
        this.service = service;
    }

    @KafkaListener(topics = "bank.notifications.v2", groupId = "bank-notification-delivery-v1")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {
        MessageDTO messageDTO = objectMapper.readValue(record.value(), MessageDTO.class);
        KafkaMessageDTO kafkaMessageDTO = new KafkaMessageDTO(messageDTO.clientId(),
                messageDTO.text(), record.partition(), record.offset());
        service.addMessages(kafkaMessageDTO);
    }

    @KafkaListener(topics = "bank.notifications.v2", groupId = "bank-notification-audit-v1")
    public void collect(ConsumerRecord<String, String> record) throws JsonProcessingException {
        MessageDTO messageDTO = objectMapper.readValue(record.value(), MessageDTO.class);
        KafkaMessageDTO kafkaMessageDTO = new KafkaMessageDTO(messageDTO.clientId(),
                messageDTO.text(), record.partition(), record.offset());
        service.addAudit(kafkaMessageDTO);
    }
}
