# Отдельный пример: три посылки обрабатываются одновременно

Служба обработки посылок отправляет статус во внешний HTTP-сервис. Один синхронный запрос удерживает свой поток до ответа. Мы создадим три consumer одного listener, чтобы запросы из трёх partitions могли выполняться одновременно.

Тест удержит HTTP-ответы и дождётся **трёх начавшихся запросов**. Если consumer только один, он застрянет на первом запросе и два остальных не начнутся. Поэтому этот пример показывает наблюдаемый эффект concurrency, а не просто настройку с числом 3.

Пример использует тот же стек, что и задача: Java 21, Spring Kafka, настоящий Kafka в Testcontainers и синхронный JDK HttpClient. В нём нет банковского REST API и списка SMS.

Из корня javaLearnAI:

```shell
mkdir -p /tmp/parcel-concurrency-demo/src/main/java/demo
mkdir -p /tmp/parcel-concurrency-demo/src/main/resources
mkdir -p /tmp/parcel-concurrency-demo/src/test/java/demo
cp mvnw /tmp/parcel-concurrency-demo/mvnw
cp -R .mvn /tmp/parcel-concurrency-demo/
```

Создай следующие файлы в `/tmp/parcel-concurrency-demo`. В рабочий `src` банковского решения их не копируй.

## `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>demo</groupId><artifactId>parcel-concurrency-demo</artifactId><version>1.0.0</version>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <dependencyManagement><dependencies>
    <dependency>
      <groupId>org.testcontainers</groupId><artifactId>testcontainers-bom</artifactId><version>2.0.5</version><type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>3.5.16</version><type>pom</type><scope>import</scope>
    </dependency>
  </dependencies></dependencyManagement>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId></dependency>
    <dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-kafka</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>testcontainers-junit-jupiter</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build><plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version>
      <configuration><release>21</release><parameters>true</parameters></configuration>
    </plugin>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version></plugin>
  </plugins></build>
</project>
```

Новых зависимостей для HTTP нет: клиент и сервер тестового шлюза входят в JDK. BOM сохраняют согласованные версии Spring и Testcontainers, как в предыдущих задачах.

## `src/main/java/demo/ParcelApplication.java`

```java
package demo;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
public class ParcelApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParcelApplication.class, args);
    }

    @Bean
    NewTopic topic() {
        return TopicBuilder.name("parcel.forward.v1").partitions(3).replicas(1).build();
    }
}
```

Три partitions дают работу трём consumer. Replication factor 1 нужен для нашего одного Kafka-брокера; он не управляет числом потоков обработки.

## `src/main/java/demo/ParcelForwarder.java`

```java
package demo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ParcelForwarder {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();
    private final URI gateway;
    private final AtomicInteger processed = new AtomicInteger();

    public ParcelForwarder(@Value("${demo.gateway-url}") String gatewayUrl) {
        this.gateway = URI.create(gatewayUrl);
    }

    @KafkaListener(topics = "parcel.forward.v1", groupId = "parcel-forwarders-demo", concurrency = "3")
    public void forward(ConsumerRecord<String, String> record) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(gateway)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(record.value()))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Gateway returned " + response.statusCode());
        }
        processed.incrementAndGet();
    }

    public int processed() { return processed.get(); }
}
```

`@Value` просит Spring взять адрес из свойства `demo.gateway-url` и передать строку в конструктор. Код превращает её в `URI`; тест подставит адрес своего сервера. Это путь «настройка → Spring → аргумент конструктора → исходящий HTTP-запрос».

`concurrency="3"` создаёт три consumer одного метода и одной группы. Объект `ParcelForwarder` общий; `request` — локальная переменная отдельного вызова. Каждый вызов строит свой запрос. Общие `HttpClient` и `URI` используются повторно, счётчик обновляется атомарно.

`send()` ждёт ответ именно в текущем consumer-потоке. Другие два consumer в это время могут выполнять свои вызовы. Счётчик увеличивается после успешного ответа, поэтому пока ответы удерживаются, он остаётся нулём. В банковской задаче вместо счётчика будет хранение обработанных уведомлений.

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

Group id указан у listener. URL шлюза ниже предоставит тест через `@DynamicPropertySource`; жёстко заданного порта приложения нет.

## `src/test/java/demo/ParcelConcurrencyTest.java`

```java
package demo;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class ParcelConcurrencyTest {
    static final CountDownLatch ARRIVED = new CountDownLatch(3);
    static final CountDownLatch RELEASE = new CountDownLatch(1);
    static final java.util.concurrent.ExecutorService HTTP_THREADS = Executors.newCachedThreadPool(
            Thread.ofPlatform().daemon().name("parcel-http-test-", 0).factory());
    static final HttpServer GATEWAY = startGateway();

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1");

    static HttpServer startGateway() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(HTTP_THREADS);
            server.createContext("/status", exchange -> {
                try {
                    exchange.getRequestBody().readAllBytes();
                    ARRIVED.countDown();
                    boolean released;
                    try {
                        released = RELEASE.await(15, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    exchange.sendResponseHeaders(released ? 204 : 503, -1);
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
        properties.add("demo.gateway-url", () -> "http://127.0.0.1:" + GATEWAY.getAddress().getPort() + "/status");
    }

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ParcelForwarder forwarder;

    @AfterAll
    static void cleanup() {
        RELEASE.countDown();
        GATEWAY.stop(0);
        HTTP_THREADS.shutdownNow();
    }

    @Test
    void threeRequestsStartBeforeAnyHttpReplyIsReleased() throws Exception {
        try {
            for (int partition = 0; partition < 3; partition++) {
                kafka.send("parcel.forward.v1", partition, "parcel-" + partition, "READY")
                        .get(10, TimeUnit.SECONDS);
            }
            assertTrue(ARRIVED.await(5, TimeUnit.SECONDS), "Нужны три начавшихся HTTP-запроса");
            assertEquals(0, forwarder.processed());
        } finally {
            RELEASE.countDown();
        }
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertEquals(3, forwarder.processed()));
    }
}
```

`CountDownLatch(3)` здесь — счётчик трёх пришедших запросов: каждый HTTP-обработчик вызывает `countDown()`. Когда запросов три, `await(...)` возвращает `true`. Второй latch пока удерживает все ответы. Тест открывает его в `finally`, чтобы освободить обработчики даже при ошибке проверки. Это инфраструктура теста; писать такую инфраструктуру для банковского решения тебе не нужно.

Тестовый producer явно направляет по записи в каждую partition, чтобы сценарий был управляемым. В банковском POST используется обычная отправка по key, как раньше. Потоки `HTTP_THREADS` обслуживают тестовый сервер; параллелизм приложения создаёт `concurrency` listener.

## Запуск

Из корня `javaLearnAI`, после создания файлов:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f /tmp/parcel-concurrency-demo/pom.xml clean test
```

Ожидается один успешный тест. Он показывает три одновременных HTTP-запроса, а не измеряет скорость компьютера. Kafka и локальный HTTP-сервер запускаются тестом автоматически при работающем Docker Engine.

Для сравнения можешь в этом отдельном примере заменить только `concurrency = "3"` на `concurrency = "1"`, оставив три partitions. Запусти тот же тест: он упадёт с сообщением «Нужны три начавшихся HTTP-запроса». Первый запрос ждёт ответа, а единственный consumer пока не может начать следующие. Затем верни `"3"`: три consumer смогут начать три запроса до освобождения ответов. Оба варианта проверены тренером 24.10.2026; писать или менять тест не требуется.

Источники механизмов: [Spring Kafka concurrency](https://docs.spring.io/spring-kafka/reference/3.3/kafka/receiving-messages/listener-annotation.html), [JDK HttpClient.send](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html).
