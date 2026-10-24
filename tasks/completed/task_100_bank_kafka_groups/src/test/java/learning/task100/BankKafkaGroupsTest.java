package learning.task100;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Проверяет внешний контракт и настоящую доставку Kafka; имена классов приложения свободны. */
@Testcontainers
@SpringBootTest(properties = {
        "logging.level.org.apache.kafka=WARN",
        "logging.level.org.springframework.kafka=WARN"
})
@AutoConfigureMockMvc
@Timeout(60)
class BankKafkaGroupsTest {
    private static final String TOPIC = "bank.notifications.v2";
    private static final Map<String, String> GROUPS = Map.of(
            "delivery", "bank-notification-delivery-v1",
            "audit", "bank-notification-audit-v1");
    private static final List<TopicPartition> PARTITIONS = List.of(
            new TopicPartition(TOPIC, 0), new TopicPartition(TOPIC, 1), new TopicPartition(TOPIC, 2));
    private static final AtomicLong IDS = new AtomicLong(4_000_000_000L);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    @DynamicPropertySource
    static void address(DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void bothGroupsAreConfiguredAndReceiveAllThreePartitions() {
        var actualGroups = registry.getListenerContainers().stream()
                .map(MessageListenerContainer::getGroupId).collect(Collectors.toSet());
        assertTrue(actualGroups.containsAll(GROUPS.values()),
                "Нужны обе группы " + GROUPS.values() + "; найдены " + actualGroups);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            for (String view : GROUPS.keySet()) {
                var assigned = new HashSet<TopicPartition>();
                for (var container : containers(view)) {
                    assertTrue(container.isRunning());
                    if (container.getAssignedPartitions() != null) assigned.addAll(container.getAssignedPartitions());
                }
                assertEquals(Set.copyOf(PARTITIONS), assigned, "Назначение partitions для " + view);
            }
        });
    }

    @Test
    void applicationCreatesThreePartitionsWithOneReplica() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            var topic = admin.describeTopics(List.of(TOPIC)).allTopicNames().get(10, TimeUnit.SECONDS).get(TOPIC);
            assertEquals(3, topic.partitions().size());
            topic.partitions().forEach(partition -> assertEquals(1, partition.replicas().size()));
        }
    }

    @Test
    void onePostPublishesOneJsonRecordAndBothViewsExposeItsActualMetadata() throws Exception {
        long id = IDS.incrementAndGet();
        String text = "  Поступление: 500 ₽, \"бонус\"\nНовая строка  ";
        try (var probe = observeNewRecords()) {
            postAccepted(id, text);
            var records = new ArrayList<ConsumerRecord<String, String>>();
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                probe.poll(Duration.ofMillis(200)).forEach(records::add);
                assertEquals(1, records.size(), "POST публикует одну запись, не отдельную запись для каждой группы");
            });
            var record = records.getFirst();
            assertEquals(Long.toString(id), record.key());
            assertEquals(payload(id, text), json.readTree(record.value()));
            assertTrue(record.partition() >= 0 && record.partition() < 3);
            assertTrue(record.offset() >= 0);
            assertEquals(0, probe.poll(Duration.ofMillis(500)).count());
            var expected = json.createArrayNode().add(observed(id, text, record.partition(), record.offset()));
            for (String view : GROUPS.keySet()) eventuallyEquals(view, id, expected);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void independentProducerCanDeliverToEveryPartition(int partition) throws Exception {
        long id = IDS.incrementAndGet();
        JsonNode event = publishDirectly(id, "Запись partition " + partition, partition);
        for (String view : GROUPS.keySet()) eventuallyEquals(view, id, json.createArrayNode().add(event));
    }

    @Test
    void sameClientKeyKeepsOnePartitionAndItsOffsetOrderInBothGroups() throws Exception {
        long id = IDS.incrementAndGet();
        List<String> texts = List.of("Первое", "Второе", "Третье");
        try (var probe = observeNewRecords()) {
            for (String text : texts) postAccepted(id, text);
            var records = new ArrayList<ConsumerRecord<String, String>>();
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                probe.poll(Duration.ofMillis(200)).forEach(records::add);
                assertEquals(3, records.size());
            });
            assertEquals(1, records.stream().map(ConsumerRecord::partition).distinct().count());
            var expected = json.createArrayNode();
            long previousOffset = -1;
            for (int i = 0; i < records.size(); i++) {
                var record = records.get(i);
                assertEquals(Long.toString(id), record.key());
                assertEquals(payload(id, texts.get(i)), json.readTree(record.value()));
                assertTrue(record.offset() > previousOffset);
                previousOffset = record.offset();
                expected.add(observed(id, texts.get(i), record.partition(), record.offset()));
            }
            for (String view : GROUPS.keySet()) eventuallyEquals(view, id, expected);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"delivery", "audit"})
    void stoppingOneGroupDoesNotStopOrFillItsViewThroughTheOtherGroup(String stoppedView) throws Exception {
        long id = IDS.incrementAndGet();
        String activeView = stoppedView.equals("delivery") ? "audit" : "delivery";
        var stopped = containers(stoppedView);
        stopped.forEach(container -> container.stop());
        JsonNode received;
        try {
            postAccepted(id, "Независимая обработка");
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(1, read(activeView, id).size()));
            received = read(activeView, id);
            assertEquals("Независимая обработка", received.get(0).get("text").asText());
            assertEquals(json.createArrayNode(), read(stoppedView, id),
                    "У каждой группы своё хранилище; POST и другая группа его не заполняют");
        } finally {
            stopped.forEach(container -> container.start());
        }
        eventuallyEquals(stoppedView, id, received);
        assertEquals(received, read(activeView, id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"delivery", "audit"})
    void unknownClientReturnsEmptyArray(String view) throws Exception {
        assertEquals(json.createArrayNode(), read(view, IDS.incrementAndGet()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 120})
    void validTextLengthBoundariesReachBothGroups(int length) throws Exception {
        long id = IDS.incrementAndGet();
        String text = "я".repeat(length);
        postAccepted(id, text);
        awaitBothTexts(id, text);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void positiveLongBoundariesReachBothGroups(long id) throws Exception {
        postAccepted(id, "Граница long");
        awaitBothTexts(id, "Граница long");
    }

    static Stream<String> invalidBodies() {
        return Stream.of("{}", "null", "{\"text\":\"Сообщение\"}",
                "{\"clientId\":null,\"text\":\"Сообщение\"}",
                "{\"clientId\":0,\"text\":\"Сообщение\"}",
                "{\"clientId\":-1,\"text\":\"Сообщение\"}",
                "{\"clientId\":7}", "{\"clientId\":7,\"text\":null}",
                "{\"clientId\":7,\"text\":\"\"}",
                "{\"clientId\":7,\"text\":\"  \\n\\t  \"}",
                "{\"clientId\":7,\"text\":\"" + "x".repeat(121) + "\"}", "{broken", "");
    }

    @ParameterizedTest(name = "invalid POST #{index}")
    @MethodSource("invalidBodies")
    void invalidPostReturns400AndPublishesNothing(String body) throws Exception {
        try (var probe = observeNewRecords()) {
            mvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            assertEquals(0, probe.poll(Duration.ofMillis(500)).count());
        }
    }

    static Stream<Arguments> invalidQueries() {
        return GROUPS.keySet().stream().flatMap(view -> Stream.of("", "?clientId=", "?clientId=0", "?clientId=-1",
                "?clientId=abc", "?clientId=9223372036854775808").map(query -> Arguments.of(view, query)));
    }

    @ParameterizedTest
    @MethodSource("invalidQueries")
    void invalidClientQueryReturns400ForBothViews(String view, String query) throws Exception {
        mvc.perform(get("/api/notifications/" + view + query)).andExpect(status().isBadRequest());
    }

    private List<MessageListenerContainer> containers(String view) {
        return registry.getListenerContainers().stream().filter(c -> GROUPS.get(view).equals(c.getGroupId())).toList();
    }

    private JsonNode payload(long id, String text) throws JsonProcessingException {
        return json.readTree(json.writeValueAsString(Map.of("clientId", id, "text", text)));
    }

    private JsonNode observed(long id, String text, int partition, long offset) throws JsonProcessingException {
        return json.readTree(json.writeValueAsString(Map.of(
                "clientId", id, "text", text, "partition", partition, "offset", offset)));
    }

    private void postAccepted(long id, String text) throws Exception {
        mvc.perform(post("/api/notifications").contentType(MediaType.APPLICATION_JSON).content(payload(id, text).toString()))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
    }

    private JsonNode read(String view, long id) throws Exception {
        var response = mvc.perform(get("/api/notifications/" + view).param("clientId", Long.toString(id)))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        return json.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void eventuallyEquals(String view, long id, JsonNode expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(expected, read(view, id)));
    }

    private void awaitBothTexts(long id, String text) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var delivery = read("delivery", id);
            var audit = read("audit", id);
            assertEquals(1, delivery.size());
            assertEquals(delivery, audit);
            var event = delivery.get(0);
            assertEquals(4, event.size());
            assertEquals(id, event.get("clientId").asLong());
            assertEquals(text, event.get("text").asText());
            assertTrue(event.get("partition").asInt() >= 0 && event.get("partition").asInt() < 3);
            assertTrue(event.get("offset").asLong() >= 0);
        });
    }

    private KafkaConsumer<String, String> observeNewRecords() {
        var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false));
        consumer.assign(PARTITIONS);
        consumer.seekToEnd(PARTITIONS);
        for (var partition : PARTITIONS) consumer.position(partition, Duration.ofSeconds(10));
        return consumer;
    }

    private JsonNode publishDirectly(long id, String text, int partition) throws Exception {
        try (var producer = new KafkaProducer<String, String>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"))) {
            var metadata = producer.send(new ProducerRecord<>(TOPIC, partition, Long.toString(id),
                    payload(id, text).toString())).get(10, TimeUnit.SECONDS);
            return observed(id, text, metadata.partition(), metadata.offset());
        }
    }
}
