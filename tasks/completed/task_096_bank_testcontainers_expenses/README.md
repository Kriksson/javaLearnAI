# Задача №96 — Банк: API расходов и тестовый PostgreSQL

Сложность: базовая. Самостоятельно создаёшь код и конфигурацию в `learning.task096`; JDBC или JPA выбираешь сам. Закрепляем POST/GET, DTO, Bean Validation, identity ID, Flyway и реальные ограничения PostgreSQL.

Два новых понятия: **Testcontainers** управляет временным PostgreSQL из Java-тестов; **@DynamicPropertySource** передаёт параметры созданной базы в Spring перед запуском приложения. В этой задаче ты пишешь и приложение, и небольшую общую конфигурацию интеграционных тестов.

## Зачем менять способ проверки

В ручной практике Compose нужен, чтобы поднять приложение и базу для использования. Интеграционному тесту нужна собственная база с предсказуемым состоянием, которую можно автоматически создать и удалить.

Testcontainers — Java-библиотека, которая обращается к Docker Engine. JUnit запускает контейнер PostgreSQL, ждёт его готовности, выполняет тесты и завершает контейнер. База не использует volume твоего `bank-build-lab` и не обращается к рабочим данным.

В этой практике приложение запускается **в JVM тестов Spring Boot**, а PostgreSQL — **в Docker**. MockMvc отправляет запрос через Spring MVC без публикации HTTP-порта. Собрать Docker-образ приложения, запустить Compose или заранее собрать JAR для этих тестов не требуется.

Адрес базы определяется после старта контейнера: Docker назначает свободный порт компьютера. Его нельзя заранее вписать как `db:5432`: это адрес из сети Compose, а наш тест работает на компьютере. Testcontainers предоставляет готовый JDBC URL.

## Работающий пример на другом случае

Пример проверяет, что Spring подключился к временному PostgreSQL и получил результат SQL. Он использует те же Maven, JUnit, Spring и Testcontainers, что потребуются в задании. Создай его отдельно в `/tmp/audit-db-demo`.

Из корня учебного проекта:

```shell
mkdir -p /tmp/audit-db-demo/src/main/java/demo
mkdir -p /tmp/audit-db-demo/src/test/java/demo
cp mvnw /tmp/audit-db-demo/mvnw
cp -R .mvn /tmp/audit-db-demo/
```

### pom.xml примера

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>demo</groupId>
    <artifactId>audit-db-demo</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
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

Две новые зависимости имеют scope `test`: модуль PostgreSQL предоставляет контейнер базы, модуль JUnit управляет его жизненным циклом. Драйвер PostgreSQL подключаем отдельно — Testcontainers его не заменяет. Здесь используем Testcontainers **2.0.5**: в версии 2 имена модулей и импорт контейнера отличаются от старых примеров 1.x. Не смешивай версии.

### Приложение примера

`src/main/java/demo/DemoApplication.java`:

```java
package demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
}
```

Для этого теста отдельный main не нужен: `@SpringBootTest` создаёт Spring-контекст по указанному классу. В своём API ты создаёшь обычное запускаемое приложение Spring Boot.

### Тест примера

`src/test/java/demo/AuditDatabaseTest.java`:

```java
package demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = DemoApplication.class)
@Testcontainers
class AuditDatabaseTest {
    @Container
    static final PostgreSQLContainer database =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("audit_demo")
                    .withUsername("demo_user")
                    .withPassword("demo_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void readsValueFromPostgres() {
        Integer result = jdbc.queryForObject("SELECT 40 + 2", Integer.class);
        assertEquals(42, result);
    }
}
```

Разбор ключевых строк:

- `@SpringBootTest` подключает Spring и создаёт его компоненты, включая DataSource и JdbcTemplate.
- `@Testcontainers` подключает расширение JUnit, которое управляет контейнерами.
- `@Container` помечает поле, которое расширение должно запустить и остановить.
- `static` означает один контейнер на все методы этого класса тестов. Без static контейнер создаётся заново для каждого метода. Поэтому здесь static экономит время.
- `new PostgreSQLContainer(...)` описывает контейнер. Сам конструктор ещё не запускает Docker; запуск выполняет расширение JUnit.
- `withDatabaseName`, `withUsername`, `withPassword` задают настройки **временной** базы. Они не читаются из db.environment старого Compose.
- Метод с `@DynamicPropertySource` обязан быть static и принимать один `DynamicPropertyRegistry`.
- `registry.add` регистрирует настройку Spring и функцию получения её значения. `database::getJdbcUrl` — ссылка на метод: Spring вызовет его, когда потребуется URL. Аналогичный вариант — `() -> database.getJdbcUrl()`.
- Эти динамические свойства имеют приоритет над обычной конфигурацией приложения и окружением. Spring подключится к базе контейнера, а не к сохранённому URL предыдущей практики.
- `@Autowired` получает JdbcTemplate именно из созданного Spring-контекста. Тест выполняет SQL и проверяет число `42`.

Полный путь: Maven запускает JUnit → Testcontainers запускает PostgreSQL в Docker и ждёт готовности → Docker выдаёт свободный порт → DynamicPropertySource предоставляет JDBC URL/логин/пароль → Spring создаёт DataSource и JdbcTemplate → тест отправляет SQL → PostgreSQL возвращает 42 → JUnit проверяет результат → контейнер завершается.

Запуск из каталога примера:

```shell
cd /tmp/audit-db-demo
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME=/home/kriksson/IdeaProjects/javaLearnAI/.maven-home bash ./mvnw -Dmaven.repo.local=/home/kriksson/IdeaProjects/javaLearnAI/.maven-home/repository test
```

Нужен доступ этого процесса к Docker Engine. Testcontainers может также запустить свой небольшой служебный контейнер очистки Ryuk. Он отвечает за удаление ресурсов теста; не отключай его. Успех примера — один пройденный тест и результат SQL 42, подтверждённый assertion.

## Что создаёшь в задании

Приложение банка сохраняет расходы и выдаёт их по ID. Создай с нуля приложение в `src/main/java/learning/task096/`, конфигурацию, миграцию, DTO и доступ к данным. Внутренние имена классов и методов свободные.

Кроме приложения, самостоятельно создай **абстрактный класс `BankPostgresTestSupport`** в `src/test/java/learning/task096/BankPostgresTestSupport.java`. Это общая настройка тестового окружения; тесты тренера наследуются от неё. Имя необходимо для этого внешнего контракта, а имена поля контейнера и метода настройки выбираешь сам.

Требования к этой тестовой базе:

- `@SpringBootTest` поднимает твоё приложение. Можно явно указать свой стартовый класс через classes; без него Spring ищет конфигурацию в текущем пакете.
- `@Testcontainers` включён; отсутствие Docker приводит к ошибке, а не к skipped-тестам. Параллельный запуск тестов не включай.
- Ровно одно static-поле типа `org.testcontainers.postgresql.PostgreSQLContainer` с `@Container`, образ `postgres:17-alpine` (допустим префикс docker.io/library/). Логин, пароль и имя временной базы выбираешь сам.
- Ровно один static-метод с `@DynamicPropertySource` и параметром DynamicPropertyRegistry. Он регистрирует три настройки соединения из контейнера: spring.datasource.url, spring.datasource.username, spring.datasource.password.
- Без ручных start/stop, фиксированных host-портов, постоянных volumes, повторного использования контейнера и `@Transactional` на тестовой базе. Жизненным циклом управляет JUnit.
- Не создавай таблицу вручную и не вставляй строки в тестовой базе. Схема появляется через Flyway. Тесты тренера очищают только свою таблицу перед каждым тестом.

В примере контейнер находится прямо в конкретном тесте. В задании ты выносишь ту же настройку в абстрактный родительский класс: наследник получает @SpringBootTest, @Testcontainers, поле контейнера и метод динамических свойств. Тесты тренера добавляют MockMvc и отправляют запросы твоему API. Писать заново их проверки не нужно; дополнительные собственные тесты можешь добавить отдельно.

Подключи в pom две показанные зависимости Testcontainers 2.0.5 с test scope, Spring Boot Test, драйвер PostgreSQL и нужные зависимости приложения/Flyway. Не подключай H2. Для этой практики Spring Boot Testcontainers integration module не нужен: настройки передаются напрямую через DynamicPropertySource.

## HTTP-контракт

### POST /bank/expenses

JSON:

```json
{"description":"Coffee","amountMinor":250}
```

Расход учитывается в минимальных денежных единицах, например в копейках: 250 означает 2 рубля 50 копеек. Используем целые числа, чтобы не вводить ошибки дробной арифметики.

`description`: обязательная непустая строка, не только пробелы/табуляция/переносы строк, длина 1–100 символов включительно. Допустимые пробелы по краям и Unicode сохраняй без изменения.

`amountMinor`: обязательное целое число от 1 до Long.MAX_VALUE включительно; Java-тип должен позволять null для валидации отсутствующего значения и значения больше int. Ноль и отрицательные суммы не являются расходом.

Успех: `201 Created`, заголовок `Location: /bank/expenses/<id>`, JSON ровно из трёх полей:

```json
{"id":1,"description":"Coffee","amountMinor":250}
```

ID выдаёт PostgreSQL; он положительный и разный для разных вставок. Одинаковые описания допускаются. Отсутствующее/null/пустое/невалидное тело и нарушение ограничений полей — `400` без записи строки. Формат JSON ошибки свободный.

### GET /bank/expenses/{id}

Для существующей строки — `200`, application/json и те же три поля. Читай актуальное состояние БД: внешние SQL-изменения сразу видны при следующем запросе, после удаления строки возвращается `404`.

Положительный отсутствующий ID — `404`. Нулевой/отрицательный, нечисловой ID или число вне диапазона long — `400`. ID больше Integer.MAX_VALUE должны работать.

Все маршруты публичные, без Security и CSRF в этой практике. Создание и чтение закрепляют уже изученные операции; новая часть — самостоятельная настройка тестового PostgreSQL.

## Миграция и конфигурация

Flyway — единственный создатель схемы. Первая миграция: `src/main/resources/db/migration/V1__create_expense_entries.sql`, без стартовых данных.

| Колонка expense_entries | Ограничения |
|---|---|
| id | BIGINT GENERATED BY DEFAULT AS IDENTITY, PRIMARY KEY |
| description | VARCHAR(100), NOT NULL, CHECK запрещает пустую строку и строку из обычных пробелов |
| amount_minor | BIGINT, NOT NULL, CHECK больше нуля |

Для CHECK описания пригодится уже знакомая SQL-функция btrim: она удаляет обычные пробелы с краёв. Проверку пустого результата формулируешь сам. Проверка Java дополнительно запрещает строку из табуляции/переносов. Ограничения БД нужны потому, что SQL может выполняться и в обход API.

JDBC или JPA выбираешь сам. При подключённом JPA используй ddl-auto=validate и spring.jpa.open-in-view=false. Явно отобрази имена таблицы и колонок; JSON DTO использует amountMinor, SQL — amount_minor.

Конфигурацию соединения приложение читает через стандартные spring.datasource.*. Тестовая база переопределит их динамическими значениями. Не переопределяй полученный DataSource своим хардкодом и не подключайся к старой базе Compose.

## Примеры поведения

1. POST Coffee/250 → 201, выданный ID и Location. GET этого ID → 200 с сохранёнными значениями.
2. POST Rent/0 → 400; число строк не изменилось. Описание длиной 100 и сумма 2147483648 → 201 без переполнения int.
3. GET положительного отсутствующего ID → 404; GET /bank/expenses/0 → 400. После SQL-обновления созданной строки GET отражает новые значения.

## Файлы и запуск

Ты создаёшь:

- `src/main/java/learning/task096/`: стартовый класс, контроллер, DTO и доступ к данным; каждый самостоятельный класс отдельно.
- `src/main/resources/application.properties` и миграцию по указанному пути.
- `src/test/java/learning/task096/BankPostgresTestSupport.java`: общую настройку тестового PostgreSQL по объяснённому механизму.
- Нужные зависимости в Maven, исходная версия Java остаётся 21.

Тесты тренера: `src/test/java/learning/task096/BankExpenseApiTest.java`. 12 тестов проверяют настройки Testcontainers и подключение к его реальному PostgreSQL, Flyway/identity/ограничения SQL, POST/GET и Location, независимые ID, ошибки JSON/валидации/URL, Unicode и пробелы, границы длины и long, актуальные SQL-данные и 404 после удаления.

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

До создания зависимостей и BankPostgresTestSupport тесты ожидаемо **не компилируются**. Это исходное состояние задачи, не ошибка тестов. Сначала подключи зависимости и создай тестовую базу, затем реализуй приложение и миграцию. После этого возможные ошибки запуска контекста или API покажут следующий недостающий шаг.

Ручной Docker Compose для проверки не запускай. При доступном Docker Engine JUnit сам создаст тестовую базу. Тесты не используют постоянные данные, не отключают Docker-проверку и не должны засчитываться по skipped-результату. Логи Maven и surefire — в target/surefire-reports. Первая загрузка библиотек и Ryuk может занять больше времени.

Критерий готовности: все 12 тестов прошли, приложение хранит данные в реальном PostgreSQL, ты сам настроил static @Container и DynamicPropertySource; ошибок/пропусков нет. Решение ученика прошло все проверки.

Материал: [PostgreSQL в Testcontainers](https://java.testcontainers.org/modules/databases/postgres/), [JUnit 5 и жизненный цикл контейнера](https://java.testcontainers.org/test_framework_integration/junit_5/), [динамические настройки Spring](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/dynamic-property-sources.html).

Проверка материала тренера 12.10.2026: отдельный пример прошёл 1 тест; все 12 тестов задания прошли на временном приложении через Testcontainers 2.0.5 и Docker Engine 29.9.0, без ошибок и пропусков. Полная проверка временного приложения с загруженными зависимостями — 8,021 секунды. Временные исходники удалены, рабочие файлы решения создаёт ученик.

Проверено 12.10.2026: все 12 тестов прошли на PostgreSQL через Testcontainers, без ошибок и пропусков; полный clean test — 11,027 секунды. Задача засчитана.
