package learning.task099;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Контрактные тесты. Имена классов и методов приложения не фиксируются. */
@Testcontainers
@SpringBootTest(properties = {
        "logging.level.org.apache.kafka=WARN",
        "logging.level.org.springframework.kafka=WARN"
})
@AutoConfigureMockMvc
@Timeout(60)
class BankKafkaNotificationsTest {
    private static final String TOPIC = "bank.notifications.v1";
    private static final String GROUP = "bank-notifications-v1";
    private static final TopicPartition PARTITION = new TopicPartition(TOPIC, 0);
    private static final AtomicLong IDS = new AtomicLong(3_000_000_000L);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    @DynamicPropertySource
    static void kafkaAddress(DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired KafkaListenerEndpointRegistry listeners;

    @BeforeEach
    void waitForRealListenerAssignment() {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertFalse(listeners.getListenerContainers().isEmpty(), "Нужен @KafkaListener");
            assertTrue(listeners.getListenerContainers().stream().anyMatch(container ->
                    GROUP.equals(container.getGroupId()) && container.isRunning()
                            && container.getAssignedPartitions() != null
                            && container.getAssignedPartitions().contains(PARTITION)),
                    "Listener группы " + GROUP + " должен получить partition " + PARTITION);
        });
    }

    @Test
    void applicationCreatesTopicWithOnePartitionAndOneReplica() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            var topic = admin.describeTopics(List.of(TOPIC)).allTopicNames()
                    .get(10, TimeUnit.SECONDS).get(TOPIC);
            assertEquals(1, topic.partitions().size());
            assertEquals(1, topic.partitions().getFirst().replicas().size());
        }
    }

    @Test
    void postPublishesJsonWithClientKeyAndPreservesLongAndText() throws Exception {
        long clientId = IDS.incrementAndGet();
        String text = "  Поступление: 500 ₽, \"бонус\"\nСтрока 2  ";
        try (var probe = observeNewRecords()) {
            postAccepted(clientId, text);
            var received = new ArrayList<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>>();
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                probe.poll(Duration.ofMillis(200)).forEach(received::add);
                assertEquals(1, received.size(), "Один POST должен опубликовать одну запись");
                var record = received.getFirst();
                assertEquals(Long.toString(clientId), record.key());
                assertEquals(0, record.partition());
                assertEquals(event(clientId, text), json.readTree(record.value()));
            });
            assertEquals(0, probe.poll(Duration.ofMillis(500)).count(), "Лишняя публикация");
        }
    }

    @Test
    void listenerProcessesEventPublishedByIndependentProducer() throws Exception {
        long clientId = IDS.incrementAndGet();
        publishDirectly(clientId, "Срок вклада заканчивается");
        eventuallyContains(clientId, "Срок вклада заканчивается");
        assertEquals(json.createArrayNode(), read(clientId + 1_000_000));
    }

    @Test
    void completeHttpKafkaListenerHttpPathWorks() throws Exception {
        long clientId = IDS.incrementAndGet();
        postAccepted(clientId, "Карта готова");
        eventuallyContains(clientId, "Карта готова");
    }

    @Test
    void getForUnknownClientReturnsEmptyArray() throws Exception {
        assertEquals(json.createArrayNode(), read(IDS.incrementAndGet()));
    }

    @Test
    void listenerKeepsOrderAndSeparatesClients() throws Exception {
        long first = IDS.incrementAndGet();
        long second = IDS.incrementAndGet();
        try (var producer = independentProducer()) {
            for (String text : List.of("Первое", "Второе", "Третье")) {
                producer.send(new ProducerRecord<>(TOPIC, Long.toString(first),
                        event(first, text).toString())).get(10, TimeUnit.SECONDS);
            }
            producer.send(new ProducerRecord<>(TOPIC, Long.toString(second),
                    event(second, "Другой клиент").toString())).get(10, TimeUnit.SECONDS);
        }
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertEquals(json.createArrayNode().add(event(first, "Первое"))
                        .add(event(first, "Второе")).add(event(first, "Третье")), read(first)));
        eventuallyContains(second, "Другой клиент");
    }

    @Test
    void postDoesNotWriteProcessedListWhileListenerIsStopped() throws Exception {
        long clientId = IDS.incrementAndGet();
        var containers = List.copyOf(listeners.getListenerContainers());
        containers.forEach(container -> container.stop());
        try {
            postAccepted(clientId, "Обработает listener");
            assertEquals(json.createArrayNode(), read(clientId),
                    "POST публикует в Kafka; список изменяет только listener");
        } finally {
            containers.forEach(container -> container.start());
        }
        eventuallyContains(clientId, "Обработает listener");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 120})
    void acceptsTextLengthBoundaries(int length) throws Exception {
        long clientId = IDS.incrementAndGet();
        String text = "я".repeat(length);
        postAccepted(clientId, text);
        eventuallyContains(clientId, text);
    }

    @Test
    void acceptsSmallestAndLargestPositiveClientIds() throws Exception {
        postAccepted(1, "Минимальный ID");
        postAccepted(Long.MAX_VALUE, "Максимальный ID");
        eventuallyContains(1, "Минимальный ID");
        eventuallyContains(Long.MAX_VALUE, "Максимальный ID");
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                "{}", "null",
                "{\"text\":\"Уведомление\"}",
                "{\"clientId\":null,\"text\":\"Уведомление\"}",
                "{\"clientId\":0,\"text\":\"Уведомление\"}",
                "{\"clientId\":-1,\"text\":\"Уведомление\"}",
                "{\"clientId\":7}",
                "{\"clientId\":7,\"text\":null}",
                "{\"clientId\":7,\"text\":\"\"}",
                "{\"clientId\":7,\"text\":\"  \\n\\t  \"}",
                "{\"clientId\":7,\"text\":\"" + "x".repeat(121) + "\"}",
                "{broken", "");
    }

    @ParameterizedTest(name = "invalid body #{index}")
    @MethodSource("invalidBodies")
    void invalidRequestReturns400AndPublishesNothing(String body) throws Exception {
        try (var probe = observeNewRecords()) {
            mvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
            assertEquals(0, probe.poll(Duration.ofMillis(500)).count(),
                    "Отклонённый запрос не должен создавать событие");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?clientId=", "?clientId=0", "?clientId=-1",
            "?clientId=abc", "?clientId=9223372036854775808"})
    void invalidOrMissingClientQueryReturns400(String query) throws Exception {
        mvc.perform(get("/api/notifications" + query)).andExpect(status().isBadRequest());
    }

    private JsonNode event(long clientId, String text) throws com.fasterxml.jackson.core.JsonProcessingException {
        // Нормализуем числовые JSON-узлы: малый ID после readTree представлен IntNode.
        return json.readTree(json.writeValueAsString(Map.of("clientId", clientId, "text", text)));
    }

    private void postAccepted(long clientId, String text) throws Exception {
        mvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON)
                        .content(event(clientId, text).toString()))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
    }

    private JsonNode read(long clientId) throws Exception {
        var response = mvc.perform(get("/api/notifications").param("clientId", Long.toString(clientId)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        return json.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void eventuallyContains(long clientId, String text) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertEquals(json.createArrayNode().add(event(clientId, text)), read(clientId)));
    }

    private KafkaConsumer<String, String> observeNewRecords() {
        var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false));
        consumer.assign(List.of(PARTITION));
        consumer.seekToEnd(List.of(PARTITION));
        consumer.position(PARTITION, Duration.ofSeconds(10));
        return consumer;
    }

    private KafkaProducer<String, String> independentProducer() {
        return new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"));
    }

    private void publishDirectly(long clientId, String text) throws Exception {
        try (var producer = independentProducer()) {
            producer.send(new ProducerRecord<>(TOPIC, Long.toString(clientId),
                    event(clientId, text).toString())).get(10, TimeUnit.SECONDS);
        }
    }
}
