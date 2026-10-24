package learning.task102;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import learning.task102.DTO.KafkaMessageDTO;
import learning.task102.DTO.MessageBody;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.protocol.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class BankService {

    @Value("${bank.sms-gateway-url}")
    private URI gatewayUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;

    public BankService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    private final ConcurrentLinkedQueue<KafkaMessageDTO> queue = new ConcurrentLinkedQueue<>();

    public void sendPost(ConsumerRecord<String, String> record) throws IOException, InterruptedException {
        MessageBody messageBody = objectMapper.readValue(record.value(), MessageBody.class);
        String json = objectMapper.writeValueAsString(messageBody);
        HttpRequest request = HttpRequest.newBuilder(gatewayUrl)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .setHeader("Content-type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            KafkaMessageDTO kafkaMessageDTO =
                    new KafkaMessageDTO(messageBody.clientId(), messageBody.text(),
                            record.partition(), record.offset());
            queue.add(kafkaMessageDTO);
        } else {
            throw new IOException("Ответ HTTP -> " + response.statusCode());
        }
    }

    public void sendMessage(MessageBody messageBody)
            throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        String json = objectMapper.writeValueAsString(messageBody);
        kafkaTemplate.send("bank.sms.v1", String.valueOf(messageBody.clientId()), json)
                .get(10, TimeUnit.SECONDS);
    }

    public List<KafkaMessageDTO> getMessagesById(Long clientId) {
        List<KafkaMessageDTO> result = new ArrayList<>();
        for (KafkaMessageDTO kafkaMessageDTO : queue) {
            if (kafkaMessageDTO.clientId().equals(clientId)) {
                result.add(kafkaMessageDTO);
            }
        }
        return result;
    }
}
