# Отдельный пример: шлюз печати этикеток посылок

У сервиса есть событие о посылке. Он отправляет JSON в HTTP-шлюз печати. Шлюз иногда отвечает `503`. Если второй вызов успешен, счётчик обработанных событий увеличивается; если он тоже неудачен, событие переносится в `parcel.labels.v1.failed`.

**В этом примере две попытки: первая и один повтор.** В банковской задаче понадобятся три попытки. Так можно увидеть, что второй аргумент FixedBackOff считает именно дополнительные повторы.

Проект полностью отдельный. Создай каталог `/tmp/parcel-retry-demo`. Все пять файлов ниже относятся к нему. Для копирования уже имеющегося Maven Wrapper из корня `javaLearnAI`:

```shell
mkdir -p /tmp/parcel-retry-demo
cp mvnw /tmp/parcel-retry-demo/
cp -R .mvn /tmp/parcel-retry-demo/
```

Каталоги для Java-файлов и ресурсов создай по указанным путям. Код этого примера не размещается среди банковских исходников.

## `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>demo</groupId>
    <artifactId>parcel-retry-demo</artifactId>
    <version>1.0</version>
    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId><artifactId>testcontainers-bom</artifactId>
                <version>2.0.5</version><type>pom</type><scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId>
                <version>3.5.16</version><type>pom</type><scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.22.2</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId><artifactId>testcontainers-kafka</artifactId><scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId><artifactId>testcontainers-junit-jupiter</artifactId><scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version>
                <configuration><release>21</release><parameters>true</parameters></configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version>
                <configuration><useModulePath>false</useModulePath></configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## `src/main/java/demo/parcel/ParcelApplication.java`

```java
package demo.parcel;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@SpringBootApplication
public class ParcelApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParcelApplication.class, args);
    }

    @Bean
    NewTopic labels() {
        return TopicBuilder.name("parcel.labels.v1").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic failedLabels() {
        return TopicBuilder.name("parcel.labels.v1.failed").partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler labelErrors(KafkaTemplate<String, String> template) {
        var recovery = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition("parcel.labels.v1.failed", record.partition()));
        recovery.setFailIfSendResultIsError(true);
        return new DefaultErrorHandler(recovery, new FixedBackOff(150L, 1L));
    }
}
```

`recovery` задаёт действие после исчерпания попыток. Lambda выбирает topic ошибок и переносит номер исходной partition. Сама публикация сохраняет исходные key/value и добавляет стандартные headers.

`FixedBackOff(150L, 1L)` означает задержку 150 мс и один дополнительный вызов после первого отказа. Bean `DefaultErrorHandler` получает recoverer и эту политику. Spring Boot связывает bean с автоматически настроенным контейнером listener; отдельный аргумент в методе listener для него не нужен.

`setFailIfSendResultIsError(true)` включает проверку результата публикации в topic ошибок: неудача публикации передаётся наружу.

## `src/main/java/demo/parcel/ParcelForwarder.java`

```java
package demo.parcel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ParcelForwarder {
    private final URI gateway;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();
    private final AtomicInteger completed = new AtomicInteger();

    public ParcelForwarder(@Value("${demo.label-gateway-url}") String gatewayUrl) {
        this.gateway = URI.create(gatewayUrl);
    }

    @KafkaListener(topics = "parcel.labels.v1", groupId = "parcel-labelers-demo", concurrency = "3")
    public void receive(ConsumerRecord<String, String> record) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(gateway)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(record.value(), StandardCharsets.UTF_8))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Label gateway HTTP " + response.statusCode());
        }
        completed.incrementAndGet();
    }

    public int completed() {
        return completed.get();
    }
}
```

Здесь **один HTTP-вызов на один вызов метода**. На `503` выбрасывается IOException. Оно выходит из listener, и контейнерный обработчик вызывает listener повторно с той же записью. Счётчик увеличивается только после успешного ответа. AtomicInteger нужен из-за трёх consumer-потоков.

Полный URL приходит так: свойство `demo.label-gateway-url` → Spring подставляет строку в аргумент с `@Value` → конструктор создаёт URI → метод использует его при сборке запроса. Во время теста URL содержит случайный порт. HttpClient переиспользуется, а HttpRequest создаётся для каждого вызова.

## `src/main/resources/application.properties`

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.acks=all
```

## `src/test/java/demo/parcel/ParcelRetryTest.java`

```java
package demo.parcel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "logging.level.org.apache.kafka=WARN",
        "logging.level.org.springframework.kafka=WARN"
})
@Timeout(45)
class ParcelRetryTest {
    static final String SOURCE = "parcel.labels.v1";
    static final String FAILED = "parcel.labels.v1.failed";
    static final ObjectMapper JSON = new ObjectMapper();
    static final ConcurrentHashMap<String, AtomicInteger> ATTEMPTS = new ConcurrentHashMap<>();
    static final java.util.concurrent.ExecutorService HTTP_THREADS = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon().name("parcel-label-gateway-", 0).factory());
    static final HttpServer GATEWAY = startGateway();

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    static HttpServer startGateway() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(HTTP_THREADS);
            server.createContext("/labels", exchange -> {
                try {
                    var body = JSON.readTree(exchange.getRequestBody());
                    String label = body.get("label").asText();
                    int attempt = ATTEMPTS.computeIfAbsent(label, ignored -> new AtomicInteger()).incrementAndGet();
                    int status = label.equals("temporary") && attempt >= 2 ? 204 : 503;
                    exchange.sendResponseHeaders(status, -1);
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void addresses(DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        properties.add("demo.label-gateway-url",
                () -> "http://127.0.0.1:" + GATEWAY.getAddress().getPort() + "/labels");
    }

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ParcelForwarder forwarder;

    @BeforeEach
    void waitForThreeConsumers() {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var group = admin.describeConsumerGroups(List.of("parcel-labelers-demo")).all()
                        .get(5, TimeUnit.SECONDS).get("parcel-labelers-demo");
                assertEquals(ConsumerGroupState.STABLE, group.state());
                assertEquals(3, group.members().size());
                group.members().forEach(member -> assertEquals(1, member.assignment().topicPartitions().size()));
            });
        }
    }

    @AfterAll
    static void cleanup() {
        GATEWAY.stop(0);
        HTTP_THREADS.shutdownNow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"temporary", "broken"})
    void retriesOrPublishesOriginalRecordToFailedTopic(String label) throws Exception {
        var target = new TopicPartition(FAILED, 2);
        try (var probe = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false))) {
            probe.assign(List.of(target));
            probe.seekToEnd(List.of(target));
            probe.position(target, Duration.ofSeconds(10));
            int completedBefore = forwarder.completed();
            String key = "parcel-84";
            String value = JSON.writeValueAsString(Map.of("parcelId", 84, "label", label));
            var original = kafka.send(SOURCE, 2, key, value).get(10, TimeUnit.SECONDS);

            if (label.equals("temporary")) {
                await().atMost(Duration.ofSeconds(10)).untilAsserted(
                        () -> assertEquals(completedBefore + 1, forwarder.completed()));
                assertEquals(0, probe.poll(Duration.ofMillis(500)).count());
            } else {
                var received = new ArrayList<ConsumerRecord<String, String>>();
                await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                    probe.poll(Duration.ofMillis(200)).forEach(received::add);
                    assertEquals(1, received.size());
                });
                var failed = received.getFirst();
                assertEquals(2, failed.partition());
                assertEquals(key, failed.key());
                assertEquals(value, failed.value());
                assertEquals(SOURCE, new String(failed.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC)
                        .value(), StandardCharsets.UTF_8));
                assertEquals(original.getRecordMetadata().offset(), ByteBuffer.wrap(
                        failed.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET).value()).getLong());
                assertEquals(completedBefore, forwarder.completed());
                assertEquals(0, probe.poll(Duration.ofMillis(500)).count());
            }
            assertEquals(2, ATTEMPTS.get(label).get());
        }
    }
}
```

Сервер и его счётчики принадлежат тестовой инфраструктуре. Весь JSON события передаётся через Kafka и затем HTTP. Для `temporary` сервер отвечает `503 → 204`, для `broken` — `503 → 503`.

Тестовый consumer читает topic ошибок независимо от приложения. Он проверяет исходные key/value и header исходного offset. Сам offset записи в topic ошибок будет другим. Проверка стабильной группы из трёх участников нужна, чтобы сценарий начинался после их присоединения.

## Запуск и разбор

Из корня `javaLearnAI` после создания файлов, при работающем Docker Engine:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f /tmp/parcel-retry-demo/pom.xml clean test
```

Ожидаются **2 успешных теста**. Kafka и локальный HTTP-шлюз поднимаются автоматически. Исключения в логах при `503` ожидаемы: ими запускается повтор; оценивай итог Maven.

Проследи путь: событие → первый HTTP-вызов → IOException → DefaultErrorHandler → повтор того же listener → успех либо recoverer → topic ошибок. Внутри listener нет цикла повторов. Для основной задачи бюджет увеличится с одного дополнительного повтора до двух, а результат успеха будет сохраняться для GET.

Источники: [Spring Kafka 3.3: обработчик ошибок и DLT](https://docs.spring.io/spring-kafka/reference/3.3/kafka/annotation-error-handling.html), [Spring Boot 3.5: подключение CommonErrorHandler](https://docs.spring.io/spring-boot/3.5/reference/messaging/kafka.html).
