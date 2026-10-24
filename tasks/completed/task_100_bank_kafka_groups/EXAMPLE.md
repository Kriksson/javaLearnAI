# Отдельный пример: одна посылка, две группы

Пример публикует строку `READY`. Группы отслеживания и статистики независимо получают одну запись вместе с её `partition` и `offset`. Здесь нет банковских DTO, HTTP и истории уведомлений: хранится только последнее наблюдение каждой группы. В задаче №100 потребуются отдельные списки всех обработанных уведомлений.

**Какую проблему решаем:** событие о посылке нужно двум независимым операциям. Отслеживание должно обновить статус, а статистика — учесть событие. Если их consumer находятся в одной группе, они делят partitions и событие может получить только один из них. Две разные группы позволяют каждой операции прочитать одну и ту же запись.

**Зачем нужны partitions при росте нагрузки:** если одна из операций перестанет успевать, её работу можно разделить между несколькими consumer одной группы. Код обработки остаётся тем же. Три partitions позволяют задействовать до трёх consumer этой группы; одна partition досталась бы только одному. Настройка `concurrency` определяет число consumer для listener, а число partitions ограничивает их параллельное чтение одного topic.

В приведённом ниже примере на каждую группу приходится один consumer. Он демонстрирует независимость групп и метаданные, а не увеличение скорости. Три partitions сами по себе его параллельнее не делают. Подробный пример ограничения «приходит 30 сообщений, один consumer успевает 10» разобран в разделе 2 условия.

Создай пример отдельно, из корня `javaLearnAI`:

```shell
mkdir -p /tmp/parcel-groups-demo/src/main/java/demo
mkdir -p /tmp/parcel-groups-demo/src/main/resources
mkdir -p /tmp/parcel-groups-demo/src/test/java/demo
cp mvnw /tmp/parcel-groups-demo/mvnw
cp -R .mvn /tmp/parcel-groups-demo/
```

Затем сам создай следующие файлы в `/tmp/parcel-groups-demo`. В `src` банковского решения пример не размещай.

## `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>demo</groupId><artifactId>parcel-groups-demo</artifactId><version>1.0.0</version>
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
      <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId>
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

Как в №99, BOM согласуют версии. В `dependencies` версии уже можно не повторять. Testcontainers остаётся 2.0.5, Spring Kafka берётся из Boot BOM 3.5.16.

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
        return TopicBuilder.name("parcel.state.v1").partitions(3).replicas(1).build();
    }
}
```

В topic теперь три partitions. Подключённый Boot создаёт `KafkaAdmin`, который применяет bean `NewTopic`. Отдельное имя Java-метода не определяет ни группу, ни имя topic.

## `src/main/java/demo/SeenParcel.java`

```java
package demo;

public record SeenParcel(String status, int partition, long offset) {}
```

Это объект для наблюдения обработанной записи. `partition` и `offset` берутся из Kafka, а не присылаются в значении сообщения.

## `src/main/java/demo/ParcelReceivers.java`

```java
package demo;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ParcelReceivers {
    private final AtomicReference<SeenParcel> tracking = new AtomicReference<>();
    private final AtomicReference<SeenParcel> statistics = new AtomicReference<>();

    @KafkaListener(topics = "parcel.state.v1", groupId = "parcel-tracking-demo")
    public void track(ConsumerRecord<String, String> record) {
        tracking.set(new SeenParcel(record.value(), record.partition(), record.offset()));
    }

    @KafkaListener(topics = "parcel.state.v1", groupId = "parcel-statistics-demo")
    public void collect(ConsumerRecord<String, String> record) {
        statistics.set(new SeenParcel(record.value(), record.partition(), record.offset()));
    }

    public SeenParcel tracking() { return tracking.get(); }
    public SeenParcel statistics() { return statistics.get(); }
}
```

Два listener подписаны на одинаковый topic, но `groupId` различаются, поэтому каждый получает запись независимо. Это два метода приёма событий, HTTP-контроллерами они не являются.

`ConsumerRecord<String, String>` содержит строковые key/value и метаданные. `record.key()` возвращает ключ, `record.value()` — значение, `record.partition()` — номер partition, `record.offset()` — позицию. В отличие от параметра `String` из №99, здесь доступна не только полезная нагрузка.

В этом примере `AtomicReference` безопасно хранит последнее наблюдение для проверки одного сообщения. Она заменяет предыдущее значение. Для банковской истории используй накапливающее потокобезопасное хранилище.

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

Общую `consumer.group-id` здесь не задаём: две группы явно указаны в аннотациях. `earliest` определяет старт при отсутствии сохранённой позиции группы, а не заставляет перечитывать всё при каждом запросе.

## `src/test/java/demo/ParcelGroupsTest.java`

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
class ParcelGroupsTest {
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.9.1")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

    @DynamicPropertySource
    static void address(DynamicPropertyRegistry properties) {
        properties.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ParcelReceivers receivers;

    @Test
    void bothGroupsSeeOneRecordWithTheSameMetadata() throws Exception {
        var metadata = kafka.send("parcel.state.v1", "parcel-9", "READY")
                .get(10, TimeUnit.SECONDS).getRecordMetadata();
        var expected = new SeenParcel("READY", metadata.partition(), metadata.offset());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertEquals(expected, receivers.tracking());
            assertEquals(expected, receivers.statistics());
        });
    }
}
```

Producer публикует запись один раз. Его `RecordMetadata` содержит фактическое место записи; оба listener должны увидеть эти же значения. Awaitility ждёт обработку независимо от подтверждения отправки. Инфраструктура теста запускает настоящий Kafka и передаёт адрес в Spring до создания producer/consumer.

Путь данных: строка `READY` и ключ `parcel-9` → serializer → одна partition topic → две независимые группы → два `ConsumerRecord` одной записи → два объекта `SeenParcel` с одинаковыми метаданными.

## Запуск

Из корня `javaLearnAI`, после создания файлов:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f /tmp/parcel-groups-demo/pom.xml clean test
```

Ожидается один успешный тест. Требуется работающий Docker Engine; отдельный Kafka вручную не запускается.

Основа механизмов: [Spring Kafka 3.3: listener и ConsumerRecord](https://docs.spring.io/spring-kafka/reference/3.3/kafka/receiving-messages/listener-annotation.html), [Apache Kafka: модель partitions и групп](https://kafka.apache.org/39/getting-started/introduction/).
