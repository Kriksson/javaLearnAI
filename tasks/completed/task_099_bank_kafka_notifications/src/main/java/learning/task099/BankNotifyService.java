package learning.task099;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class BankNotifyService {

    private final Queue<NotificationRequest> queue = new ConcurrentLinkedQueue<>();

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    public BankNotifyService(KafkaTemplate<String, String> kafkaTemplate,  ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationRequest body) throws JsonProcessingException, ExecutionException,
            InterruptedException, TimeoutException {
        String json = objectMapper.writeValueAsString(body);
        kafkaTemplate.send("bank.notifications.v1", body.clientId().toString(), json)
                .get(10, TimeUnit.SECONDS);
    }

    public void addQueue(String message) throws JsonProcessingException {
        NotificationRequest request = objectMapper.readValue(message, NotificationRequest.class);
        this.queue.add(request);
    }

    public List<NotificationRequest> getQueueById(long clientId) {
        List<NotificationRequest> result = new ArrayList<>();

        for (NotificationRequest request : this.queue) {
            if (request.clientId() ==  clientId) {
                result.add(request);
            }
        }
        return result;
    }
}
