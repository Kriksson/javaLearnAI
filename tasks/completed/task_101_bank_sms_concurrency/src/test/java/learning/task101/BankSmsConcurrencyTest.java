package learning.task101;

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
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties = {
        "logging.level.org.apache.kafka=WARN",
        "logging.level.org.springframework.kafka=WARN"
})
@AutoConfigureMockMvc
@Timeout(60)
class BankSmsConcurrencyTest {
    private static final String TOPIC = "bank.sms.v1";
    private static final String GROUP = "bank-sms-senders-v1";
    private static final List<TopicPartition> PARTITIONS = List.of(
            new TopicPartition(TOPIC, 0), new TopicPartition(TOPIC, 1), new TopicPartition(TOPIC, 2));
    private static final AtomicLong IDS = new AtomicLong(5_000_000_000L);
    private static final SmsGatewayStub GATEWAY = SmsGatewayStub.start();

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    @DynamicPropertySource
    static void addresses(DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        properties.add("bank.sms-gateway-url", GATEWAY::url);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void waitForAssignmentAndReleasePreviousGatewayBlock() {
        GATEWAY.release();
        assertTrue(registry.getListenerContainers().stream().anyMatch(c -> GROUP.equals(c.getGroupId())),
                "Нужен listener группы " + GROUP);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var assigned = new HashSet<TopicPartition>();
                for (MessageListenerContainer container : registry.getListenerContainers()) {
                    if (GROUP.equals(container.getGroupId())) {
                        assertTrue(container.isRunning());
                        if (container.getAssignedPartitions() != null) assigned.addAll(container.getAssignedPartitions());
                    }
                }
                assertEquals(Set.copyOf(PARTITIONS), assigned);
                // Один consumer тоже может читать все partitions. Ждём завершения
                // присоединения всех трёх, прежде чем удерживать HTTP-ответы.
                var group = admin.describeConsumerGroups(List.of(GROUP)).all()
                        .get(5, TimeUnit.SECONDS).get(GROUP);
                assertEquals(ConsumerGroupState.STABLE, group.state());
                assertEquals(3, group.members().size());
                group.members().forEach(member -> assertEquals(1, member.assignment().topicPartitions().size()));
            });
        }
    }

    @AfterAll
    static void stopGateway() { GATEWAY.close(); }

    @Test
    void topicHasThreePartitionsAndThreeRealConsumerMembers() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            var topic = admin.describeTopics(List.of(TOPIC)).allTopicNames().get(10, TimeUnit.SECONDS).get(TOPIC);
            assertEquals(3, topic.partitions().size());
            topic.partitions().forEach(partition -> assertEquals(1, partition.replicas().size()));
            var group = admin.describeConsumerGroups(List.of(GROUP)).all().get(10, TimeUnit.SECONDS).get(GROUP);
            assertEquals(3, group.members().size(), "concurrency=3: нужны три consumer в одной группе");
            group.members().forEach(member -> assertEquals(1, member.assignment().topicPartitions().size()));
        }
    }

    @Test
    void postPublishesOneRecordAndGatewayReceivesTheOriginalJson() throws Exception {
        long id = IDS.incrementAndGet();
        String text = "  Код: \"1234\"\nПроверка ₽  ";
        try (var probe = observeNewRecords()) {
            postAccepted(id, text);
            var record = readOne(probe);
            assertEquals(Long.toString(id), record.key());
            assertEquals(payload(id, text), json.readTree(record.value()));
            assertEquals(0, probe.poll(Duration.ofMillis(500)).count());
            JsonNode expected = json.createArrayNode().add(observed(id, text, record.partition(), record.offset()));
            eventuallyEquals(id, expected);
            assertEquals(expected, read(id), "Повторный GET не удаляет запись");
            assertGatewayCall(id, text);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void everyPartitionForwardsAnIndependentProducerRecordToTheGateway(int partition) throws Exception {
        long id = IDS.incrementAndGet();
        JsonNode expected = publishDirectly(id, "Сообщение partition " + partition, partition);
        eventuallyEquals(id, json.createArrayNode().add(expected));
        assertGatewayCall(id, "Сообщение partition " + partition);
    }

    @Test
    void threePartitionsReachTheGatewayBeforeAnyGatewayReplyIsReleased() throws Exception {
        long first = IDS.incrementAndGet(), second = IDS.incrementAndGet(), third = IDS.incrementAndGet();
        var block = GATEWAY.block(Set.of(first, second, third), null, 3);
        JsonNode one, two, three;
        try {
            one = publishDirectly(first, "Первый клиент", 0);
            two = publishDirectly(second, "Второй клиент", 1);
            three = publishDirectly(third, "Третий клиент", 2);
            assertTrue(block.awaitArrivals(5),
                    "Все три HTTP-запроса должны начаться до ответа шлюза: проверь concurrency=3 и отсутствие общей блокировки");
            for (long id : List.of(first, second, third)) assertEquals(json.createArrayNode(), read(id));
        } finally {
            block.open();
        }
        eventuallyEquals(first, json.createArrayNode().add(one));
        eventuallyEquals(second, json.createArrayNode().add(two));
        eventuallyEquals(third, json.createArrayNode().add(three));
    }

    @Test
    void anotherPartitionProgressesWhileOneGatewayRequestIsBlocked() throws Exception {
        long slow = IDS.incrementAndGet(), fast = IDS.incrementAndGet();
        var block = GATEWAY.block(Set.of(slow), null, 1);
        JsonNode delayed;
        try {
            delayed = publishDirectly(slow, "Долгий ответ", 0);
            assertTrue(block.awaitArrivals(5));
            JsonNode quick = publishDirectly(fast, "Быстрый ответ", 1);
            eventuallyEquals(fast, json.createArrayNode().add(quick));
            assertEquals(json.createArrayNode(), read(slow));
        } finally {
            block.open();
        }
        eventuallyEquals(slow, json.createArrayNode().add(delayed));
    }

    @Test
    void samePartitionKeepsProcessingOrderWhileAnotherPartitionWorks() throws Exception {
        long id = IDS.incrementAndGet(), markerId = IDS.incrementAndGet();
        var block = GATEWAY.block(Set.of(id), "Первое", 1);
        JsonNode first, second;
        try {
            first = publishDirectly(id, "Первое", 0);
            assertTrue(block.awaitArrivals(5));
            second = publishDirectly(id, "Второе", 0);
            JsonNode marker = publishDirectly(markerId, "Другая partition", 1);
            eventuallyEquals(markerId, json.createArrayNode().add(marker));
            await().during(Duration.ofMillis(300)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                assertEquals(1, GATEWAY.callsFor(id).size(),
                        () -> "Следующая запись той же partition ждёт завершения первой: " + GATEWAY.callsFor(id));
                assertEquals(json.createArrayNode(), read(id));
            });
        } finally {
            block.open();
        }
        eventuallyEquals(id, json.createArrayNode().add(first).add(second));
        assertEquals(List.of("Первое", "Второе"), GATEWAY.callsFor(id).stream()
                .map(call -> call.body().get("text").asText()).toList());
    }

    @Test
    void postAcknowledgesKafkaWithoutWaitingForTheGatewayReply() throws Exception {
        long id = IDS.incrementAndGet();
        var block = GATEWAY.block(Set.of(id), null, 1);
        var requestThread = Executors.newSingleThreadExecutor();
        JsonNode expected;
        try (var probe = observeNewRecords()) {
            var response = requestThread.submit(() -> mvc.perform(post("/api/sms-requests")
                    .contentType(MediaType.APPLICATION_JSON).content(payload(id, "Асинхронная обработка").toString()))
                    .andReturn().getResponse());
            var accepted = response.get(5, TimeUnit.SECONDS);
            assertEquals(202, accepted.getStatus());
            assertEquals("", accepted.getContentAsString());
            assertTrue(block.awaitArrivals(5));
            assertEquals(json.createArrayNode(), read(id));
            var record = readOne(probe);
            expected = json.createArrayNode().add(observed(id, "Асинхронная обработка", record.partition(), record.offset()));
        } finally {
            block.open();
            requestThread.shutdownNow();
        }
        eventuallyEquals(id, expected);
        assertGatewayCall(id, "Асинхронная обработка");
    }

    @Test
    void unknownClientReturnsEmptyArray() throws Exception {
        assertEquals(json.createArrayNode(), read(IDS.incrementAndGet()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 120})
    void textLengthBoundariesAreAccepted(int length) throws Exception {
        long id = IDS.incrementAndGet();
        String text = "я".repeat(length);
        postAccepted(id, text);
        awaitText(id, text);
        assertGatewayCall(id, text);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void positiveLongBoundariesAreAccepted(long id) throws Exception {
        postAccepted(id, "Граница long");
        awaitText(id, "Граница long");
        assertGatewayCall(id, "Граница long");
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
            mvc.perform(post("/api/sms-requests").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            assertEquals(0, probe.poll(Duration.ofMillis(500)).count());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?clientId=", "?clientId=0", "?clientId=-1", "?clientId=abc", "?clientId=9223372036854775808"})
    void invalidClientQueryReturns400(String query) throws Exception {
        mvc.perform(get("/api/sms-requests" + query)).andExpect(status().isBadRequest());
    }

    private JsonNode payload(long id, String text) throws JsonProcessingException {
        return json.readTree(json.writeValueAsString(Map.of("clientId", id, "text", text)));
    }

    private JsonNode observed(long id, String text, int partition, long offset) throws JsonProcessingException {
        return json.readTree(json.writeValueAsString(Map.of(
                "clientId", id, "text", text, "partition", partition, "offset", offset)));
    }

    private void postAccepted(long id, String text) throws Exception {
        mvc.perform(post("/api/sms-requests").contentType(MediaType.APPLICATION_JSON).content(payload(id, text).toString()))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
    }

    private JsonNode read(long id) throws Exception {
        var response = mvc.perform(get("/api/sms-requests").param("clientId", Long.toString(id)))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        return json.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void eventuallyEquals(long id, JsonNode expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertEquals(expected, read(id)));
    }

    private void awaitText(long id, String text) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var records = read(id);
            assertEquals(1, records.size());
            var event = records.get(0);
            assertEquals(4, event.size());
            assertTrue(event.get("clientId").isIntegralNumber());
            assertEquals(id, event.get("clientId").asLong());
            assertEquals(text, event.get("text").asText());
            assertTrue(event.get("partition").asInt() >= 0 && event.get("partition").asInt() < 3);
            assertTrue(event.get("offset").asLong() >= 0);
        });
    }

    private void assertGatewayCall(long id, String text) throws Exception {
        var calls = GATEWAY.callsFor(id);
        assertEquals(1, calls.size(), "Один успешный вызов шлюза на запись в этой задаче");
        var call = calls.getFirst();
        assertEquals("POST", call.method());
        assertNotNull(call.contentType());
        assertTrue(MediaType.parseMediaType(call.contentType()).isCompatibleWith(MediaType.APPLICATION_JSON));
        assertEquals(payload(id, text), call.body());
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

    private ConsumerRecord<String, String> readOne(KafkaConsumer<String, String> consumer) {
        var received = new ArrayList<ConsumerRecord<String, String>>();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            consumer.poll(Duration.ofMillis(200)).forEach(received::add);
            assertEquals(1, received.size());
        });
        return received.getFirst();
    }

    private JsonNode publishDirectly(long id, String text, int partition) throws Exception {
        try (var producer = new KafkaProducer<String, String>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all"))) {
            var metadata = producer.send(new ProducerRecord<>(TOPIC, partition, Long.toString(id), payload(id, text).toString()))
                    .get(10, TimeUnit.SECONDS);
            return observed(id, text, metadata.partition(), metadata.offset());
        }
    }
}
