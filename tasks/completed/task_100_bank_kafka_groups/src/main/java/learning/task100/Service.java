package learning.task100;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import learning.task100.DTO.KafkaMessageDTO;
import learning.task100.DTO.MessageDTO;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@org.springframework.stereotype.Service
public class Service {

    private final Queue<KafkaMessageDTO> audit = new ConcurrentLinkedQueue<>();
    private final Queue<KafkaMessageDTO> messages = new ConcurrentLinkedQueue<>();

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    public Service(KafkaTemplate<String, String> kafkaTemplate,  ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(MessageDTO messageDTO) throws JsonProcessingException, ExecutionException,
            InterruptedException, TimeoutException {
        String json = objectMapper.writeValueAsString(messageDTO);
        kafkaTemplate.send("bank.notifications.v2", String.valueOf(messageDTO.clientId()), json)
                .get(10, TimeUnit.SECONDS);
    }

    public void addAudit(KafkaMessageDTO kafkaMessageDTO) {
        audit.add(kafkaMessageDTO);
    }

    public void addMessages(KafkaMessageDTO kafkaMessageDTO) {
        messages.add(kafkaMessageDTO);
    }

    public List<KafkaMessageDTO> getMessagesById(Long clientId) {
        List<KafkaMessageDTO> result = new ArrayList<>();
        for (KafkaMessageDTO kafkaMessageDTO : messages) {
            if (kafkaMessageDTO.clientId().equals(clientId)) {
                result.add(kafkaMessageDTO);
            }
        }
        return result;
    }

    public List<KafkaMessageDTO> getAuditsById(Long clientId) {
        List<KafkaMessageDTO> result = new ArrayList<>();
        for (KafkaMessageDTO kafkaMessageDTO : audit) {
            if (kafkaMessageDTO.clientId().equals(clientId)) {
                result.add(kafkaMessageDTO);
            }
        }
        return result;
    }

}
