# Задача №98 — Банк: полезные сообщения в логах

**Сложность:** базовая. Один небольшой шаг после Actuator.

**Формат:** спроектируй и собери приложение с нуля в `learning.task098`. Приложение, настройки, миграцию и общую тестовую базу создаёшь ты. JDBC или JPA выбираешь сам. Тесты тренера уже подготовлены.

**Закрепляем:** REST, параметры запроса, актуальные данные PostgreSQL, Flyway и Testcontainers.

**Новые понятия:** SLF4J с параметрами сообщений; уровни логирования и их фильтрация. MDC, JSON-логи и сборщики логов в этой задаче не нужны.

## Зачем нужен этот шаг

Actuator показывает состояние приложения, а журнал помогает увидеть, какую операцию оно выполнило и почему отказало в конкретном запросе. Нужен небольшой осмысленный набор событий, по которому можно восстановить результат операции. HTTP-ответ остаётся для клиента, запись в журнале — для диагностики приложения.

SLF4J — интерфейс, через который Java-код отправляет сообщения в систему логирования. Logback — реализация, которая обрабатывает события и выводит их. В нашем Spring Boot web starter уже есть стандартный logging starter; отдельную вторую реализацию логирования добавлять не нужно. Тесты перехватывают события Logback, сохраняя реальные уровень, шаблон и аргументы.

| Уровень | Назначение в этой практике |
|---|---|
| DEBUG | Подробности для отладки; обязательных DEBUG-событий нет |
| INFO | Успешно выполненная бизнес-операция |
| WARN | Отклонённый запрос с числовым значением вне допустимого диапазона |
| ERROR | Неожиданный сбой; отдельный сценарий ERROR сейчас не вводится |

При уровне INFO сообщения DEBUG отфильтровываются, а INFO/WARN/ERROR доступны. При уровне WARN событие INFO не должно доходить до appender. Это управляет журналом; HTTP-результат успешного запроса не меняется.

```java
private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);
log.info("event=delivery_read id={}", deliveryId);
```

`Logger` — тип из `org.slf4j`, `LoggerFactory` создаёт логгер с именем Java-класса. Такое имя связывает событие с местом в коде и позволяет настроить уровень для всего пакета. `{}` — место для отдельного аргумента: SLF4J сохраняет шаблон и значение, а Logback формирует итоговую строку. Для id=7 получится `event=delivery_read id=7`. Конкатенацию строк и `String.format` для динамических значений здесь не используем: цель практики — освоить именно параметры SLF4J. Не передавай exception как аргумент предупреждения о штатном отсутствии записи.

## Работающий пример: статус доставки

Пример читает одну доставку через Spring JDBC из настоящего PostgreSQL, пишет INFO при нахождении и WARN при отсутствии. Он использует Maven, Spring Web, Flyway, PostgreSQL и Testcontainers. В банковской задаче будет другая операция — подсчёт с фильтром.

Создай пример отдельно, например в `/tmp/logging-demo`. Из корня учебного репозитория:

```shell
mkdir -p /tmp/logging-demo/src/main/java/demo
mkdir -p /tmp/logging-demo/src/main/resources/db/migration
mkdir -p /tmp/logging-demo/src/test/java/demo
cp mvnw /tmp/logging-demo/mvnw
cp -R .mvn /tmp/logging-demo/
```

Затем создай следующие файлы. Пример не размещай в `src` банковского решения.

### `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>demo</groupId><artifactId>logging-demo</artifactId><version>1.0.0</version>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.22.2</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <version>3.5.16</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
      <version>3.5.16</version>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
      <version>11.7.2</version>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
      <version>11.7.2</version>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.7.13</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <version>3.5.16</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-postgresql</artifactId>
      <version>2.0.5</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-junit-jupiter</artifactId>
      <version>2.0.5</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build><plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version>
      <configuration><parameters>true</parameters></configuration>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version>
    </plugin>
  </plugins></build>
</project>
```

### `src/main/java/demo/DemoApplication.java`

```java
package demo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) { SpringApplication.run(DemoApplication.class, args); }
}
```

### `src/main/java/demo/DeliveryStatus.java`

```java
package demo;
public record DeliveryStatus(String status) {}
```

### `src/main/java/demo/DeliveryController.java`

```java
package demo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController
public class DeliveryController {
    private static final Logger log = LoggerFactory.getLogger(DeliveryController.class);
    private final JdbcTemplate sql;
    public DeliveryController(JdbcTemplate sql) { this.sql = sql; }
    @GetMapping("/deliveries/{id}")
    public ResponseEntity<DeliveryStatus> status(@PathVariable long id) {
        try {
            String status = sql.queryForObject("SELECT status FROM deliveries WHERE id=?", String.class, id);
            log.info("event=delivery_read id={}", id);
            return ResponseEntity.ok(new DeliveryStatus(status));
        } catch (EmptyResultDataAccessException missing) {
            log.warn("event=delivery_missing id={}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
```

### `src/main/resources/application.properties`

```properties
logging.level.demo=INFO
```

### `src/main/resources/db/migration/V1__create_deliveries.sql`

```sql
CREATE TABLE deliveries (id BIGINT PRIMARY KEY, status VARCHAR(20) NOT NULL);
INSERT INTO deliveries(id,status) VALUES (7,'READY');
```

### `src/test/java/demo/DeliveryLoggingTest.java`

```java
package demo;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=DemoApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class DeliveryLoggingTest {
    @Container
    static final PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("delivery_demo").withUsername("demo_user").withPassword("demo_password");
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }
    @Autowired MockMvc mvc;
    @Test
    void logsSuccessfulReadAndMissingDelivery() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(DeliveryController.class);
        ListAppender<ILoggingEvent> capture = new ListAppender<>();
        capture.setContext(logger.getLoggerContext());
        capture.start();
        logger.addAppender(capture);
        try {
            mvc.perform(get("/deliveries/7")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY"));
            assertEquals(1, capture.list.size());
            assertEquals(Level.INFO, capture.list.getFirst().getLevel());
            assertEquals("event=delivery_read id={}", capture.list.getFirst().getMessage());
            assertEquals("event=delivery_read id=7", capture.list.getFirst().getFormattedMessage());
            capture.list.clear();
            mvc.perform(get("/deliveries/999")).andExpect(status().isNotFound());
            assertEquals(1, capture.list.size());
            assertEquals(Level.WARN, capture.list.getFirst().getLevel());
            assertEquals("event=delivery_missing id=999", capture.list.getFirst().getFormattedMessage());
            assertNull(capture.list.getFirst().getThrowableProxy());
        } finally {
            logger.detachAppender(capture);
            capture.stop();
        }
    }
}
```

### Полный путь запроса и проверки

1. Testcontainers запускает PostgreSQL 17 и выдаёт адрес, логин и пароль через DynamicPropertySource. Flyway создаёт таблицу и одну доставку.
2. MockMvc вызывает `GET /deliveries/7`. Spring преобразует строку `7` из URL в `long id` и вызывает контроллер.
3. JdbcTemplate передаёт ID отдельным параметром SQL и читает статус READY.
4. `log.info` отправляет в Logback событие с шаблоном и аргументом 7; контроллер возвращает `200` и `{"status":"READY"}`.
5. Для отсутствующего ID JdbcTemplate сообщает EmptyResultDataAccessException. В этом примере она переводится в WARN без stack trace и HTTP 404.
6. ListAppender хранит события для утверждений теста. `getMessage()` возвращает исходный шаблон с `{}`, `getFormattedMessage()` — строку с подставленным ID. Appender в finally отсоединяется: он нужен только для проверки.

Настройка `logging.level.demo=INFO` относится к пакету demo и его дочерним логгерам. В задаче будет другой пакет. Имена классов, Logger и HTTP-ресурс — разные понятия: логгер класса отвечает за источник события, URL определяет маршрут.

Команда проверки примера, из корня javaLearnAI:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash /tmp/logging-demo/mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f /tmp/logging-demo/pom.xml clean test
```

## Условие и внешний контракт

Создай публичный `GET /bank/expenses/count` с необязательным параметром `minimumAmountMinor`. Ответ `200`, application/json, ровно одно поле `count` с целым числом. Считай строки `expense_entries`, для которых `amount_minor >= minimumAmountMinor`. Значение по умолчанию — 1. Поддерживай положительный long вплоть до Long.MAX_VALUE. Читай актуальную базу при каждом запросе; не меняй строки и не используй счётчик в памяти.

Примеры: расходы 99, 100, 101 → без параметра `{"count":3}`; с `minimumAmountMinor=100` → `{"count":2}`; с `minimumAmountMinor=102` → `{"count":0}`.

Для числового значения 0 или отрицательного long: `400`, пустое тело, запрос на подсчёт не выполняется. Для строки, не преобразуемой в long, включая переполнение: `400` стандартными средствами Spring; отдельное бизнес-событие для ошибок преобразования не требуется, успешное событие запрещено. Отсутствующий `/bank/missing` — 404; POST на count — 405. Spring Security в этом задании не нужен.

### Контракт бизнес-событий

Логгер относится к твоему классу внутри `learning.task098`: имя должно начинаться с `learning.task098.`. Используй SLF4J, не System.out, Java Util Logging или прямое создание событий Logback.

| Результат | Уровень | Исходный шаблон SLF4J | Аргументы по порядку |
|---|---|---|---|
| Успешный подсчёт | INFO | `event=expense_count_read minimumAmountMinor={} count={}` | числовой минимум, числовое количество |
| Числовой минимум <= 0 | WARN | `event=expense_count_rejected minimumAmountMinor={}` | числовой минимум |

На один обработанный бизнес-запрос — ровно одно из этих событий. INFO пишется после успешного чтения базы. В WARN для недопустимого числового минимума не добавляй exception/stack trace и не пиши успешное событие. Для 404/405 успешного бизнес-события нет.

Настрой `logging.level.learning.task098=INFO`. Дочерние логгеры должны наследовать этот уровень: не закрепляй INFO отдельно на каждом классе. При переключении уровня пакета на WARN INFO подавляется, WARN остаётся; API продолжает отвечать корректно. Стандартные служебные логи Spring/Hibernate допустимы; контракт относится к твоим бизнес-событиям.

Не включай в собственные логи описание расхода, Authorization header, пароль и JDBC URL. В журнале этой операции достаточно заданных числовых аргументов. Имена внутренних классов и методов свободны; точные шаблоны нужны для внешней проверки учебного приёма.

## База и самостоятельно создаваемые файлы

Flyway — единственный создатель схемы; без начальных данных. Миграция `src/main/resources/db/migration/V1__create_expense_entries.sql`:

| Колонка | Контракт |
|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY, PRIMARY KEY |
| description | VARCHAR(100), NOT NULL; CHECK запрещает пустую строку и обычные пробелы |
| amount_minor | BIGINT, NOT NULL; CHECK больше нуля |

Если выбираешь JPA: `ddl-auto=validate`, `open-in-view=false`. В тестах используется один PostgreSQL 17 через Testcontainers; ручная база или Compose не нужны. Логи бизнес-операций добавляешь сам; Actuator в этой практике не является обязательным endpoint.

Создай приложение и выбранные компоненты в `src/main/java/learning/task098/`, настройки и миграцию — в `src/main/resources/`. Не создавай доменные классы вложенными в общий Solution.

Сам создай абстрактный `src/test/java/learning/task098/BankPostgresTestSupport.java`: @SpringBootTest для своего приложения, @AutoConfigureMockMvc, @Testcontainers, один static @Container PostgreSQLContainer с образом postgres:17-alpine, один static @DynamicPropertySource с тремя настройками DataSource. Имена контейнера и метода свободны. Без фиксированных host-портов, reuse, volume, ручного start/stop, @Transactional на тестовой базе или отключения Docker-проверки. Тест тренера наследуется от этого класса.

Подключай только нужные зависимости: Spring Web, JDBC или JPA, Flyway, PostgreSQL, Spring Boot Test и два test-модуля Testcontainers. SLF4J/Logback предоставляет стандартный logging starter Spring Boot. Для этой задачи Maven-плагин не нужен среди обычных зависимостей; повторяющиеся зависимости удали самостоятельно. Java остаётся 21.

## Проверка и критерии готовности

Тест тренера: `src/test/java/learning/task098/BankExpenseLoggingApiTest.java`. 11 тестов проверяют фильтр и границы long, актуальность SQL, Flyway и ограничения схемы, статусы ошибок, шаблоны и числовые аргументы реальных событий SLF4J, INFO/WARN и фильтрацию по уровню, отсутствие лишних данных в логах.

Самостоятельно разберись, какую строку проверяет каждый тест. Дополнительные собственные тесты можешь писать в этом же пакете; не изменяй проверки тренера, чтобы обойти контракт. Запрос на подсчёт при числовом минимуме <= 0 и использование именно SLF4J дополнительно проверяются при чтении кода.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Готовность: все 11 тестов проходят без ошибок и пропусков; бизнес-события соответствуют контракту, данные читаются из временного PostgreSQL и не раскрываются в журнале. До создания приложения и BankPostgresTestSupport тесты ожидаемо не компилируются. Следующая задача не считается решённой по результату временного приложения тренера.

Материал: [SLF4J manual — Logger и параметры сообщений](https://slf4j.org/manual.html), [Logback — уровни и иерархия логгеров](https://logback.qos.ch/manual/architecture.html).


Проверка материала тренера 13.10.2026: полный отдельный пример Maven/Spring/JDBC/Flyway/PostgreSQL/SLF4J прошёл 1 тест; окончательные 11 тестов задания прошли на отдельном временном приложении с настоящим PostgreSQL 17, без ошибок и пропусков. Это проверка учебных материалов, а не зачёт решения ученика. Временная банковская реализация удалена; в src/main решение №98 не создавалось.


## Результат проверки решения

13.10.2026: все 11 интеграционных тестов прошли с реальным PostgreSQL 17 через Testcontainers на JDK 21. Failures: 0, Errors: 0, Skipped: 0. Код проверен: неверный числовой минимум отклоняется до обращения к репозиторию, успешный подсчёт получает актуальные данные, SLF4J сохраняет шаблоны и числовые аргументы, INFO/WARN наследуют уровень пакета. Исправлены повторная зависимость Web starter и Maven-плагин среди обычных зависимостей. Реализация ученика сохранена без изменений.

Проверка архива из корня репозитория:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f tasks/completed/task_098_bank_expense_logging/pom.xml clean test
```
