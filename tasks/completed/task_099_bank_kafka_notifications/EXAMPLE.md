# Работающий пример: сообщение о посылке

Это отдельный проект для изучения механизма. Одна строка отправляется через `KafkaTemplate`, настоящий брокер сохраняет её, listener принимает. REST, банковских DTO и списка уведомлений здесь нет.

## Создание отдельного каталога

Из корня `javaLearnAI`:

```shell
mkdir -p /tmp/parcel-kafka-demo/src/main/java/demo
mkdir -p /tmp/parcel-kafka-demo/src/main/resources
mkdir -p /tmp/parcel-kafka-demo/src/test/java/demo
cp mvnw /tmp/parcel-kafka-demo/mvnw
cp -R .mvn /tmp/parcel-kafka-demo/
```

Сам создай следующие файлы в `/tmp/parcel-kafka-demo`. Не копируй пример в `src` банковского решения.

## `pom.xml`

`spring-kafka` содержит Spring-интеграцию и Kafka Java client; тестовый модуль Testcontainers запускает Kafka через Docker. `spring-boot-starter-test` даёт JUnit и Awaitility, но контейнерный модуль нужно добавить отдельно.

В `dependencyManagement` импортируются BOM — списки согласованных версий библиотек. BOM сам не добавляет библиотеки в приложение: это делает раздел `dependencies`. Первым импортирован Testcontainers BOM, чтобы выбрать уже используемую нами ветку 2.0.5 вместо версии из Boot BOM. Boot BOM согласует остальные зависимости, включая SLF4J и Kafka client. Это предотвращает появление SLF4J 1.7 рядом с Logback для SLF4J 2, которое возможно при независимом подключении библиотек без управления версиями.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>demo</groupId><artifactId>parcel-kafka-demo</artifactId><version>1.0.0</version>
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
    <dependency>
      <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId><version>3.5.16</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId><version>3.3.16</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><version>3.5.16</version><scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId><artifactId>testcontainers-kafka</artifactId><version>2.0.5</version><scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId><artifactId>testcontainers-junit-jupiter</artifactId><version>2.0.5</version><scope>test</scope>
    </dependency>
  </dependencies>
  <build><plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version>
      <configuration><release>21</release><parameters>true</parameters></configuration>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version>
    </plugin>
  </plugins></build>
</project>
```

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
    NewTopic parcelTopic() {
        return TopicBuilder.name("parcel.status.v1").partitions(1).replicas(1).build();
    }
}
```

`NewTopic` описывает topic. Spring Boot создаёт `KafkaAdmin`, который применяет это описание к брокеру при старте. Имя Java-метода произвольное; имя Kafka topic находится внутри `TopicBuilder`. Один брокер допускает здесь одну реплику.

## `src/main/java/demo/ParcelReceiver.java`

```java
package demo;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ParcelReceiver {
    private final AtomicReference<String> latest = new AtomicReference<>();

    @KafkaListener(topics = "parcel.status.v1")
    public void receive(String value) {
        latest.set(value);
    }

    public String latest() {
        return latest.get();
    }
}
```

Spring обнаруживает метод с `@KafkaListener` и вызывает его при чтении записи. В Boot стандартная инфраструктура listener настраивается автоматически. Параметр `value` получает строковое значение записи после десериализации. В примере сохраняется только последняя строка; `AtomicReference` делает запись и чтение из разных потоков безопасными. Она не заменяет хранилище списка, которое нужно в банковской задаче.

## `src/main/resources/application.properties`

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=parcel-status-demo
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.acks=all
```

`bootstrap-servers` — начальные адреса для подключения к кластеру. В тесте адрес заменяется случайным адресом контейнера. `group-id` именует группу consumer, `earliest` задаёт чтение с начала при отсутствии сохранённой позиции. Сериализатор превращает строку в байты, десериализатор — обратно. JSON-строка тоже является строкой: если передаёшь JSON через `StringSerializer`, Java-объект сначала нужно явно преобразовать через Jackson.

`acks=all` означает ожидание подтверждения от всех текущих синхронных реплик; в нашем одноузловом примере реплика одна. Это подтверждение сохранения записи брокером, не выполнения listener.

## `src/test/java/demo/ParcelMessagingTest.java`

```java
package demo;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class ParcelMessagingTest {
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    @DynamicPropertySource
    static void address(DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired KafkaTemplate<String, String> template;
    @Autowired ParcelReceiver receiver;

    @Test
    void stringPassesThroughRealKafka() throws Exception {
        template.send("parcel.status.v1", "parcel-9", "READY")
                .get(10, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertEquals("READY", receiver.latest()));
    }
}
```

`@Testcontainers` включает управление контейнером через JUnit, `static @Container` запускает один брокер для класса. Используем `org.testcontainers.kafka.KafkaContainer`; старый класс из `org.testcontainers.containers` не нужен. `@DynamicPropertySource` передаёт адрес в Spring до создания producer и consumer.

`send(topic, key, value)` возвращает future. `get(10, TimeUnit.SECONDS)` ограниченно ждёт подтверждение отправки и выбрасывает исключение при ошибке. Listener выполняется независимо, поэтому затем Awaitility периодически проверяет результат в течение не более 15 секунд. Фиксированный `Thread.sleep()` не нужен.

Путь данных: тест передаёт key=`parcel-9`, value=`READY` → serializer кодирует строки → producer пишет в `parcel.status.v1` → consumer группы `parcel-status-demo` читает запись → deserializer восстанавливает `READY` → Spring вызывает `receive` → тест читает безопасно опубликованное значение через `latest()`.

## Запуск

Из корня `javaLearnAI`, после создания файлов:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f /tmp/parcel-kafka-demo/pom.xml clean test
```

Ожидается один успешный тест с настоящим Kafka в Docker. Отдельный брокер вручную не запускай. В задаче №99 перенеси понимание этого механизма на контракт уведомлений, сохранив свои имена компонентов и собственную реализацию.

Основа примера: [Spring Boot 3.5 Kafka](https://docs.spring.io/spring-boot/3.5/reference/messaging/kafka.html), [Spring Kafka: отправка](https://docs.spring.io/spring-kafka/reference/3.3/kafka/sending-messages.html), [Testcontainers Kafka](https://java.testcontainers.org/modules/kafka/).
