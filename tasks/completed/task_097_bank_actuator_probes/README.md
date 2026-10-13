# Задача №97 — Банк: диагностика готовности API

Сложность: базовая. Приложение, миграцию, зависимости и тестовую конфигурацию создаёшь с нуля в `learning.task097`. JDBC или JPA выбираешь сам. Закрепляем PostgreSQL, Flyway, DTO, актуальные SQL-данные и Testcontainers.

Новые понятия: **Spring Boot Actuator** и **health probes readiness/liveness**. В этой практике используем стандартные проверки Spring, без своего контроллера здоровья или самописного HealthIndicator.

## Для чего это нужно

В №94 проверяли готовность PostgreSQL перед запуском API. Теперь проверяем работающее приложение: процесс может оставаться запущенным, когда обязательная база недоступна и банковские операции выполнить невозможно.

Actuator добавляет служебные маршруты диагностики. HealthIndicator — компонент, который проверяет конкретную часть приложения. Когда подключён DataSource, стандартный индикатор `db` проверяет возможность обращения к базе. Общий `/actuator/health` объединяет результаты проверок.

**Readiness** отвечает: «готово ли приложение выполнять запросы сейчас?» Для нашего API PostgreSQL обязателен, поэтому включаем `db` в readiness. При сбое базы ответ будет DOWN и HTTP 503.

**Liveness** отвечает: «работает ли само приложение?» Недоступность внешней базы не означает, что JVM сломалась. Поэтому liveness не включает db и остаётся UP при таком сбое. Автоматический перезапуск API не исправит остановленный PostgreSQL.

Это диагностические ответы. Они сами не останавливают обработку запросов, не перезапускают сервисы и не чинят базу. В будущей практике pipeline сможет проверять readiness после развёртывания. Kubernetes для этого задания не нужен.

## Работающий пример на отдельном приложении

Проверим готовность сервиса отчётов с PostgreSQL. Пример использует тот же Maven, Spring Web, Actuator, JUnit, MockMvc и Testcontainers, что потребуются в задании. Создай его в отдельном `/tmp/probe-demo`:

```shell
mkdir -p /tmp/probe-demo/src/main/java/demo
mkdir -p /tmp/probe-demo/src/main/resources
mkdir -p /tmp/probe-demo/src/test/java/demo
cp mvnw /tmp/probe-demo/mvnw
cp -R .mvn /tmp/probe-demo/
```

### pom.xml

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>demo</groupId>
    <artifactId>probe-demo</artifactId>
    <version>1.0.0</version>
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
            <artifactId>spring-boot-starter-jdbc</artifactId>
            <version>3.5.16</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>3.5.16</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.13</version>
            <scope>runtime</scope>
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
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId><version>3.5.16</version></dependency><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId><version>3.5.16</version></dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.4</version>
            </plugin>
        </plugins>
    </build>
</project>
```

Новые зависимости — spring-boot-starter-actuator и Spring Web для служебных HTTP-маршрутов. JdbcTemplate/DataSource и Testcontainers уже знакомы. Драйвер PostgreSQL подключается отдельно; зависимости Testcontainers имеют test scope. Jackson указан явно в той же версии, что в учебном Maven-проекте, чтобы библиотеки JSON оставались согласованными.

### Приложение примера

`src/main/java/demo/DemoApplication.java`:

```java
package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

Контроллер `/actuator/health` писать не нужно: его предоставляет Actuator. Бизнес-контроллер своего API в задании создаёшь ты.

### application.properties

В `src/main/resources/application.properties` примера:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=never
management.endpoint.health.show-components=never
management.endpoint.health.group.readiness.include=readinessState,db
spring.datasource.hikari.connection-timeout=1000
spring.datasource.hikari.validation-timeout=1000
```

Разбор каждой настройки:

| Настройка | Назначение |
|---|---|
| exposure.include=health | Публикует через HTTP только служебный endpoint health; env, beans, mappings и остальные не открываем |
| probes.enabled=true | Включает стандартные группы liveness и readiness в нашем обычном окружении |
| show-details=never | Не возвращает детали проверок, ошибки соединения и другие внутренние значения |
| show-components=never | Не возвращает дерево компонентов; ответу не нужны внутренние компоненты |
| group.readiness.include=readinessState,db | Readiness объединяет состояние готовности Spring и проверку нашей обязательной базы |
| hikari.connection-timeout=1000 | Ограничивает ожидание соединения из пула одной секундой; значение в миллисекундах, важно для быстрой диагностики недоступной базы |
| hikari.validation-timeout=1000 | Ограничивает проверку пригодности соединения одной секундой |

`readinessState` и `livenessState` — стандартные индикаторы состояния приложения. `db` — стандартная проверка DataSource. Группа объединяет выбранные индикаторы. После успешного старта Spring выставляет готовность принимать запросы, но db дополнительно должен подтвердить соединение.

Readiness по умолчанию не включает все внешние зависимости. В нашем конкретном API база обязательна, поэтому добавляем её явно. Liveness оставляем стандартной, с `livenessState` без db.

### JUnit-тест примера

`src/test/java/demo/ProbeTest.java`:

```java
package demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class ProbeTest {
    @Container
    static final PostgreSQLContainer database =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("reports_demo")
                    .withUsername("demo_user")
                    .withPassword("demo_password");

    @DynamicPropertySource
    static void connection(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void checksBothKindsOfReadiness() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/health/readiness",
                "/actuator/health/liveness"}) {
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
    }
}
```

Тестовая конфигурация соединения знакома по №96: static-контейнер и DynamicPropertySource. `@AutoConfigureMockMvc` позволяет отправить HTTP-запрос через Spring MVC. Теперь запрос попадает в служебный endpoint Actuator.

Путь проверки: Maven запускает JUnit → Testcontainers поднимает PostgreSQL → динамические настройки подключают к нему Spring DataSource → Spring завершает запуск → MockMvc обращается к health → Actuator запускает стандартные индикаторы → db проверяет соединение → все выбранные проверки успешны → получаем HTTP 200 и status UP.

Если подключение к PostgreSQL после старта недоступно: проверка db получает ошибку → общий health и readiness дают HTTP 503 с DOWN → liveness проверяет состояние приложения и остаётся HTTP 200 с UP. После восстановления базы новые проверки снова возвращают UP; перезапуск приложения не требуется.

Запуск примера:

```shell
cd /tmp/probe-demo
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME=/home/kriksson/IdeaProjects/javaLearnAI/.maven-home bash ./mvnw -Dmaven.repo.local=/home/kriksson/IdeaProjects/javaLearnAI/.maven-home/repository test
```

Нужен работающий Docker Engine и доступ текущего процесса к docker.sock. В примере один тест проверяет три маршрута на настоящем PostgreSQL. Сбой и восстановление проверяются отдельным тестом задания, который работает только со своей временной базой.

## Условие и HTTP-контракт

Банк считает расходы и предоставляет служебные маршруты диагностики. Ты создаёшь бизнес-контроллер и конфигурацию Actuator. Служебные маршруты оставляешь стандартному Actuator.

### Бизнес-маршрут

`GET /bank/expenses/count` — публичный маршрут: `200`, application/json, единственное поле count с целым числом актуальных строк таблицы expense_entries.

Примеры: пустая таблица → `{"count":0}`; две SQL-вставки → `{"count":2}`; после удаления одной → `{"count":1}`. Читай БД при каждом запросе, без счётчика в памяти или кеша стартового значения. Отсутствующий `/bank/missing` — 404; POST на count — 405.

Создание расходов через POST из №96 здесь не требуется: данные для проверки вставляет SQL теста. Во время сбоя базы проверяем именно служебные маршруты; формат ошибки бизнес-маршрута в этот момент не регламентируется.

### Служебные маршруты

| Состояние | /actuator/health | /actuator/health/readiness | /actuator/health/liveness |
|---|---|---|---|
| API запущено, PostgreSQL доступен | 200, UP | 200, UP | 200, UP |
| Подключение к PostgreSQL недоступно после успешного старта | 503, DOWN | 503, DOWN | 200, UP |
| PostgreSQL восстановлен, API продолжает работать | 200, UP | 200, UP | 200, UP |

В ответах readiness/liveness ровно одно поле status, например:

```json
{"status":"DOWN"}
```

Корневой `/actuator/health` стандартно также возвращает список доступных групп, например `{"status":"UP","groups":["liveness","readiness"]}`. Это имена диагностических групп, а не детали базы; такое поле допустимо. Дополнительные группы в этой практике не нужны.

Не публикуй components, details, URL базы, имена пользователей или текст исключений. Параметр запроса `?show-details=always` не должен менять настроенное скрытие данных. JSON может иметь стандартный Actuator Content-Type вида application/vnd.spring-boot.actuator.v3+json.

Публикуй только endpoint health; `/actuator/env`, `/actuator/beans`, `/actuator/mappings`, `/actuator/metrics`, `/actuator/info`, `/actuator/configprops` должны давать 404. Без Spring Security в этой практике; все заданные маршруты публичные и используют основное web-приложение.

Используй стандартный DataSourceHealthIndicator и стандартные группы: readiness включает readinessState и db, liveness включает livenessState и не включает db. Не отключай проверку db, не возвращай захардкоженный UP и не создавай контроллеры вместо Actuator.

Для небольшого времени проверки явно настрой `spring.datasource.hikari.connection-timeout` в диапазоне 250–2000 миллисекунд включительно; пример использует 1000. Это время получения соединения из пула, а не интервал healthcheck Docker. Нового Dockerfile или Compose для тестов не нужно.

## База и приложение

Flyway — единственный создатель схемы. Миграция `src/main/resources/db/migration/V1__create_expense_entries.sql`, без стартовых данных:

| Колонка expense_entries | Ограничения |
|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY, PRIMARY KEY |
| description | VARCHAR(100), NOT NULL, CHECK запрещает пустую строку и строку из обычных пробелов |
| amount_minor | BIGINT, NOT NULL, CHECK больше нуля |

JDBC или JPA выбираешь сам. Если подключён JPA, Hibernate только проверяет схему через ddl-auto=validate; Open Session in View отключён. После восстановления базы строка и история Flyway сохраняются, API читает их без перезапуска контекста Spring.

Приложение в `src/main/java/learning/task097/` создаёшь самостоятельно, с обычным стартовым классом. Имена внутренних классов и методов свободные; каждому самостоятельному классу отдельный файл.

## Тестовая конфигурация

Самостоятельно создай `src/test/java/learning/task097/BankPostgresTestSupport.java`, как в №96: абстрактный класс, @SpringBootTest для своего приложения, @Testcontainers, один static @Container PostgreSQLContainer с образом postgres:17-alpine, DynamicPropertySource для трёх настроек соединения.

Имя общей тестовой базы нужно потому, что от неё наследуются тесты тренера. Поле контейнера и метод настройки называешь сам. В версии Testcontainers 2.0.5 импорт контейнера — org.testcontainers.postgresql.PostgreSQLContainer.

Жизненный цикл контейнера управляется расширением JUnit. Без фиксированного host-порта, постоянных volumes, reuse, ручного start/stop в твоей конфигурации, отключения Docker-проверки или @Transactional на тестовой базе. Схему создаёт Flyway; тесты очищают свою таблицу перед каждым случаем.

В тесте сбоя тренер временно запрещает новые подключения роли тестового приложения и закрывает её текущие соединения, затем восстанавливает доступ. Сам PostgreSQL продолжает работать, его адрес и данные сохраняются. Это внутренний механизм проверки недоступного соединения. Тебе нужно реализовать стандартную диагностику приложения, а не код имитации сбоя. Тест восстанавливает доступ к PostgreSQL в finally и не затрагивает ручные проекты Compose.

Подключаешь зависимости приложения, Flyway, PostgreSQL, Spring Boot Test и два test-модуля Testcontainers 2.0.5. Новая зависимость — spring-boot-starter-actuator той же версии Spring Boot. Источник Java остаётся 21.

## Проверка

Тест тренера: `src/test/java/learning/task097/BankHealthApiTest.java`. 10 тестов проверяют стандартные health-компоненты и состав групп, ответы UP/DOWN, скрытие деталей, ограниченную публикацию endpoints, SQL-счётчик, Flyway и ограничения таблицы, 404/405, реальный отказ подключения к PostgreSQL и восстановление с сохранением данных.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

До создания приложения, общей тестовой базы и добавления Actuator тесты ожидаемо не компилируются либо не загружают контекст. Это исходное состояние задачи. Материал и тесты не являются готовой реализацией банковского API.

Для ручного запуска можешь подключить приложение к своему учебному PostgreSQL через реальные стандартные настройки spring.datasource.* и проверить маршруты curl. Значения `1` из предыдущей задачи не являются JDBC URL: в тестах их заменял DynamicPropertySource, а обычный запуск требует настоящих параметров соединения. В тестах конфигурация должна получать адрес временной базы.

Критерий готовности: все 10 тестов прошли без ошибок и пропусков, диагностика отражает доступность обязательной базы, liveness сохраняется при её сбое и readiness восстанавливается без перезапуска API. До проверки №97 не засчитана.

Материал: [Actuator, health и probes в Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html), [Testcontainers и JUnit](https://java.testcontainers.org/test_framework_integration/junit_5/).

Проверка материала тренера 12.10.2026: отдельный Maven/Spring/Actuator/Testcontainers-пример прошёл 1 тест; все 10 тестов задания прошли на временном приложении и реальном PostgreSQL, без ошибок и пропусков. Полная проверка временного приложения — 12,585 секунды. Сбой имитировался запретом соединений роли тестовой базы с последующим восстановлением; readiness и общий health показали DOWN/503, liveness оставался UP/200, после восстановления данные сохранились. Временные исходники удалены; реализацию создаёт ученик. Начальная проверка рабочего проекта ожидаемо не компилируется до создания общей тестовой базы и подключения Actuator.


## Результат проверки решения

13.10.2026: код и конфигурация проверены на соответствие контракту; все 10 интеграционных тестов прошли с реальным PostgreSQL 17 через Testcontainers на JDK 21. Failures: 0, Errors: 0, Skipped: 0. Подтверждены актуальный SQL-счётчик, стандартный db indicator, DOWN/503 для health и readiness при отказе подключения, UP/200 для liveness и восстановление без перезапуска приложения.

Необязательные замечания к сохранённому Maven-файлу: повторное объявление spring-boot-starter-web; spring-boot-maven-plugin указан также как обычная зависимость. Конфигурация ученика сохранена без исправлений. Для ручного запуска требуется настоящий JDBC URL и параметры своей базы вместо тестовых значений.

Воспроизводимая проверка архива из корня репозитория:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" -f tasks/completed/task_097_bank_actuator_probes/pom.xml clean test
```
