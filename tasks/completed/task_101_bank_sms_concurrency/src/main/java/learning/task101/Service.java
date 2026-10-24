package learning.task101;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@org.springframework.stereotype.Service
public class Service {

    @Value("${bank.sms-gateway-url}")
    private URI gatewayURI;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    private final Queue<KafkaMessageDTO> queue = new ConcurrentLinkedQueue<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    public Service(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(SMSRequestBody body)
            throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        String json = objectMapper.writeValueAsString(body);
        kafkaTemplate.send("bank.sms.v1", String.valueOf(body.clientId()), json)
                .get(10, TimeUnit.SECONDS);
    }

    public void sendPostGateway(ConsumerRecord<String, String> record)
            throws URISyntaxException, IOException, InterruptedException {
        SMSRequestBody body = objectMapper.readValue(record.value(), SMSRequestBody.class);
        KafkaMessageDTO kafkaMessageDTO =
                new KafkaMessageDTO(body.clientId(), body.text(), record.partition(), record.offset());
        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder(gatewayURI)
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            queue.add(kafkaMessageDTO);
        }
    }

    public List<KafkaMessageDTO> getByClientId(Long clientId) {
        List<KafkaMessageDTO> result = new ArrayList<>();
        for (KafkaMessageDTO kafkaMessageDTO : queue) {
            if (kafkaMessageDTO.clientId().equals(clientId)) {
                result.add(kafkaMessageDTO);
            }
        }
        return result;
    }
}
