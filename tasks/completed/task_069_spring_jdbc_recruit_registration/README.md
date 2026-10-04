# Задача №69 — Регистрация рекрута через Spring JDBC

**Сложность:** базовая. Один небольшой шаг после чтения записи из базы.

**Формат:** спроектируй и собери приложение с нуля. Сам создай Java-файлы, SQL-схему и настройки; имена внутренних классов и методов выбираешь ты. Используй существующий учебный Maven-проект и его зависимости.

**Закрепи:** Spring MVC, JSON, Bean Validation, `201 Created`, `Location`, `409 Conflict`, конструкторное внедрение, `JdbcTemplate.query` и параметризованный SQL.

**Новое:** запись через `JdbcTemplate.update(...)` и обработка `DuplicateKeyException` при повторном первичном ключе.

## Небольшой проект

Внешний игровой каталог уже выдаёт каждому персонажу числовой ID. Гильдия получает этот ID вместе с именем и уровнем и регистрирует рекрута у себя.

Создай два endpoint:

```text
POST /guild/recruits
GET  /guild/recruits/{id}
```

`POST` записывает нового рекрута в SQL-таблицу. `GET` позволяет прочитать его после регистрации. ID передаёт клиент: автоматическая генерация ID здесь не требуется.

Один ID нельзя зарегистрировать дважды: иначе новая заявка могла бы перезаписать существующего рекрута. Разные ID могут иметь одинаковые имена. Данные хранятся в H2 в памяти и исчезают после завершения процесса.

## Материал: запись и конфликт ключа

Метод `JdbcTemplate.update` выполняет `INSERT`, `UPDATE` или `DELETE` и возвращает число изменённых строк. Значения передаются отдельно вместо `?`. Spring преобразует нарушение уникальности ключа в `DuplicateKeyException`. [Spring Framework: JDBC](https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc/core.html).

Ниже отдельный работающий пример учёта поставок книг. Для него нужны те же зависимости Spring Web, Spring JDBC и H2. Создавай пример только в отдельном проекте или пакете `demo`; в файлы решения №69 его переносить не нужно.

`demo/DeliveryDemoApplication.java`:

```java
package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeliveryDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryDemoApplication.class, args);
    }
}
```

`demo/BookDelivery.java`:

```java
package demo;

public record BookDelivery(String code, int quantity) {}
```

`demo/BookDeliveryRepository.java`:

```java
package demo;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookDeliveryRepository {
    private final JdbcTemplate jdbc;

    public BookDeliveryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean add(BookDelivery delivery) {
        try {
            int changed = jdbc.update(
                    "INSERT INTO book_deliveries (code, quantity) VALUES (?, ?)",
                    delivery.code(), delivery.quantity());
            return changed == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
}
```

- Конструктор получает `JdbcTemplate` из Spring.
- `update(...)` выполняет один `INSERT`; код и количество связываются с двумя параметрами SQL.
- Результат `1` означает, что добавлена одна строка.
- Если `code` уже существует, первичный ключ запрещает вставку. Spring выбрасывает `DuplicateKeyException`, а пример возвращает `false`.
- Здесь обработан конкретный конфликт ключа. Другие ошибки базы не стоит превращать в ответ «запись уже существует».

`demo/BookDeliveryController.java`:

```java
package demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookDeliveryController {
    private final BookDeliveryRepository repository;

    public BookDeliveryController(BookDeliveryRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/demo/deliveries")
    public ResponseEntity<Boolean> receive(@RequestBody BookDelivery delivery) {
        return repository.add(delivery)
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(409).body(false);
    }
}
```

Ресурсы **отдельного примера** — `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:delivery-demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.sql.init.mode=always
```

И `schema.sql`:

```sql
CREATE TABLE book_deliveries (
    code VARCHAR(20) PRIMARY KEY,
    quantity INTEGER NOT NULL
);
```

Запусти `DeliveryDemoApplication.main()` и дважды выполни:

```shell
curl -i -H 'Content-Type: application/json' -d '{"code":"atlas","quantity":4}' http://localhost:8080/demo/deliveries
```

Путь данных: JSON → `@RequestBody` создаёт `BookDelivery` → репозиторий связывает `atlas` и `4` с параметрами → H2 добавляет строку → `update` возвращает `1` → клиент видит `200` и `true`. Повторный запрос нарушает первичный ключ → `DuplicateKeyException` → клиент видит `409` и `false`; первая строка остаётся прежней.

В задании добавь знакомую валидацию входного DTO через `@Valid`. Успешный ответ должен содержать рекрута и иметь статус `201`, как указано ниже.

## Таблица и запуск

Рабочий пакет: `learning.task069`. Сам создай стартовый класс, модель запроса/ответа, репозиторий и контроллер; при необходимости — отдельный обработчик ошибок. Каждый самостоятельный класс находится в своём `.java`-файле. Дополнительный сервис и интерфейсы не обязательны.

Сам создай `src/main/resources/application.properties` с настройками H2 в памяти и выполнения SQL-скрипта, а также `src/main/resources/schema.sql`. При старте таблица `guild_recruits` должна существовать и быть **пустой**:

| Колонка | Тип |
|---|---|
| `id` | `BIGINT PRIMARY KEY` |
| `name` | `VARCHAR(30) NOT NULL` |
| `level` | `INTEGER NOT NULL` |

Используй имеющиеся `spring-boot-starter-jdbc` версии `${spring.boot.version}` и H2 `2.3.232` с областью `runtime`. Сохрани Java 21 и остальные зависимости проекта.

## HTTP-контракт

Запрос `POST /guild/recruits` принимает JSON с полями `id`, `name`, `level`:

- `id` обязателен и должен быть положительным значением `long`;
- `name` обязателен, содержит хотя бы один непробельный символ и имеет длину от 1 до 30 включительно по `String.length()`;
- `level` обязателен и находится в диапазоне от 1 до 100 включительно.

В проверке числовые значения передаются JSON-числами; отдельно проверяются отсутствующие поля, `null`, границы и строки, которые нельзя преобразовать в нужное число. Дополнительные поля JSON и числовые преобразования дробных значений не входят в задание.

Имя храни и возвращай **как пришло**: пробелы вокруг непустого имени, кавычки и апострофы допустимы. Ограничение длины соответствует размеру колонки; диапазон уровня отражает правила игрового каталога. ID связывает рекрута с уже существующим персонажем.

| Вход | Результат |
|---|---|
| Корректный `POST`, ID свободен | `201`, JSON с числовыми `id`, `level` и строковым `name`; `Location: /guild/recruits/{id}`; добавлена ровно одна строка |
| Корректный `POST`, ID уже в базе | `409`; существующие строки не изменились |
| Поля `POST` не проходят проверки, тело отсутствует или JSON повреждён | `400`; база не изменилась |
| `GET`, ID найден | `200`, поля актуальной строки из базы |
| `GET`, ID не найден, но помещается в `long`, включая 0 и отрицательные значения | `404` без тела |
| `GET`, ID не является числом или выходит за диапазон `long` | `400` |

Формат тела ошибки для `400` и `409` свободный. Повторная регистрация даже с теми же данными возвращает `409`. Два разных ID с одним именем допустимы.

Каждый `GET` читает базу и не изменяет её. Изменения через другое соединение должны быть видны API. Приложение не хранит собственную копию каталога в `Map` или `List`.

**Что проверяешь ты:** ограничения DTO, результат вставки, конфликт первичного ключа и отсутствие строки при чтении. Spring преобразует JSON в DTO и ID пути в `long`; знакомая Bean Validation проверяет аннотации только при подключённом `@Valid`. Ошибку преобразования запроса Spring MVC превращает в `400`.

## Примеры

1. `POST /guild/recruits` с `{"id":40,"name":"Лира","level":25}` → `201`, то же содержимое в JSON и `Location: /guild/recruits/40`. Следующий `GET /guild/recruits/40` → `200` с этой записью.
2. Повторный `POST` с `id=40`, именем `Мира` и уровнем `100` → `409`. В базе остаются `Лира` и уровень `25`.
3. `POST` с `{"id":50,"name":" ","level":0}` → `400`. Строка с ID `50` не создаётся.

## Критерии готовности и файлы

- Создай своё приложение в `src/main/java/learning/task069/`, настройки и SQL-скрипт — в `src/main/resources/`.
- Репозиторий использует внедрённый через конструктор `JdbcTemplate`. Запросы `SELECT` и `INSERT` параметризованы; SQL не склеивается со значениями пользователя. Запись выполняется через `JdbcTemplate.update(...)`.
- Конфликт ID обеспечивается первичным ключом базы и корректно переводится в `409`; существующая запись не заменяется.
- Тесты находятся в `src/test/java/learning/task069/RecruitRegistrationApiTest.java`. Они проверяют схему и пустой старт, запись и чтение, границы, ошибки JSON и валидации, повторные ID и актуальность данных из SQL.
- Все автоматические тесты проходят. Использование `update`, параметризованный SQL и конструкторное внедрение дополнительно проверяются при просмотре кода.

## Проверка и сборка

Из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Для сборки JAR замени `clean test` на `clean package`. В IntelliJ IDEA выбери Temurin 21 в Project SDK и Maven Runner, а приложение запускай через созданный тобой `main`.

Пока приложение ещё не создано, тесты ожидаемо сообщают `Unable to find a @SpringBootConfiguration`. Созданы только условие и тесты; исходные файлы и конфигурацию решения ты пишешь сам.

После реализации напиши **«Решил»**.

## Результат проверки

Задача решена и подтверждена 4 октября 2026 года: `clean test` на Temurin 21.0.12.1 — **46 тестов, 0 failures, 0 errors**. Дополнительно проверены пакет `learning.task069`, параметризованные `SELECT` и `INSERT`, запись через `JdbcTemplate.update`, конструкторное внедрение и обработка конфликта первичного ключа.

Решение перенесено сюда без изменения кода: `RecruitApplication.java`, `Recruit.java`, `RecruitsRepository.java`, `RecruitsController.java`, `application.properties`, `schema.sql` и `RecruitRegistrationApiTest.java`. Рабочие каталоги `src` освобождены для следующей задачи.
