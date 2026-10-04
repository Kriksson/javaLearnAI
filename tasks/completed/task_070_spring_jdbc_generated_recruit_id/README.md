# Задача №70 — ID рекрута создаёт база

**Сложность:** базовая. Один шаг после регистрации рекрута.

**Формат:** спроектируй и собери приложение с нуля. Сам создай Java-файлы, настройки и SQL-схему. Используй пакет `learning.task070`, текущий Maven-проект и уже подключённые зависимости.

**Закрепи:** `JdbcTemplate.update`, параметризованный `INSERT`, JSON, Bean Validation, `201 Created`, `Location` и чтение через `JdbcTemplate.query`.

**Новое:** SQL-колонка `IDENTITY` для генерации ID базой и `GeneratedKeyHolder` для возврата этого ID через Spring JDBC.

## Сценарий

Гильдия регистрирует нового рекрута и назначает ему уникальный номер. Теперь номер выдаёт база данных при вставке. После создания клиент получает ID и ссылку, по которой может получить рекрута.

Создай два endpoint:

```text
POST /guild/recruits
GET  /guild/recruits/{id}
```

У `POST` в JSON есть только поля `name` и `level`. Не назначай ID в Java и не включай его в SQL `INSERT`: значение появится при добавлении строки.

## Пример: получить ID новой книги

В отдельном проекте учёта книг можно добавить книгу и вернуть назначенный базой ID. Этот пример использует те же Spring Web, Spring JDBC и H2. Оставь его в материале; в решение №70 перенеси только идею.

`demo/LabelDemoApplication.java`:

```java
package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LabelDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelDemoApplication.class, args);
    }
}
```

`demo/LabelRequest.java`:

```java
package demo;

public record LabelRequest(String title) {}
```

`demo/LabelRepository.java`:

```java
package demo;

import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LabelRepository {
    private final JdbcTemplate jdbc;

    public LabelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long add(String title) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO book_labels (title) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            return statement;
        }, keys);
        Number id = keys.getKey();
        if (id == null) {
            throw new IllegalStateException("The database returned no generated ID");
        }
        return id.longValue();
    }
}
```

`demo/LabelController.java`:

```java
package demo;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LabelController {
    private final LabelRepository repository;

    public LabelController(LabelRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/demo/labels")
    public ResponseEntity<Long> add(@RequestBody LabelRequest request) {
        long id = repository.add(request.title());
        return ResponseEntity.created(URI.create("/demo/labels/" + id)).body(id);
    }
}
```

Настройки **отдельного примера** — `application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:labels-demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.sql.init.mode=always
```

И `schema.sql`:

```sql
CREATE TABLE book_labels (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(40) NOT NULL
);
```

Запусти `LabelDemoApplication.main()` и отправь:

```shell
curl -i -H 'Content-Type: application/json' -d '{"title":"Секретный сад"}' http://localhost:8080/demo/labels
```

Путь данных: JSON → `@RequestBody` создаёт `LabelRequest` → репозиторий связывает название с параметром `?` → H2 вставляет строку и назначает ID → `GeneratedKeyHolder` получает ключ → приложение возвращает `201`, числовой ID в JSON и `Location`.

В репозитории `prepareStatement(..., Statement.RETURN_GENERATED_KEYS)` просит JDBC вернуть ключ вставки. Лямбда передаёт подготовленный запрос обратно в `JdbcTemplate`; шаблон сам его выполняет и закрывает ресурсы. `KeyHolder` хранит полученное значение, а `longValue()` приводит номер к типу колонки `BIGINT`. `ResponseEntity.created(...)` выставляет `201` и заголовок `Location`.

## Приложение

Сам создай стартовый класс, модель входных данных, модель ответа, репозиторий и контроллер в `src/main/java/learning/task070/`. Каждый класс размещай в отдельном файле. Дополнительный `@Service` или интерфейс не требуется.

Создай `src/main/resources/application.properties` для H2 в памяти и запуска SQL-скрипта. В `src/main/resources/schema.sql` создай пустую таблицу `guild_recruits`:

| Колонка | Требование |
|---|---|
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` |
| `name` | `VARCHAR(30) NOT NULL` |
| `level` | `INTEGER NOT NULL` |

Таблица начинается пустой. Все новые ID выдаёт база данных.

Синтаксис identity-колонки для H2 описан в его [грамматике SQL](https://h2database.com/html/grammar.html).

В Maven уже подключены `spring-boot-starter-jdbc` и H2 `2.3.232`. Java 21 и остальные зависимости сохрани.

## HTTP-контракт

`POST /guild/recruits` принимает JSON с обязательными полями:

- `name` — непустое имя длиной от 1 до 30 символов включительно; строка должна содержать хотя бы один непробельный символ;
- `level` — целое число от 1 до 100 включительно.

Тесты передают `level` JSON-целым числом. Настраивать отдельное строгое преобразование дробных JSON-чисел в этой задаче не нужно.

Валидный запрос получает `201 Created`, JSON созданного рекрута с числовыми `id` и `level` и строковым `name`, а также `Location: /guild/recruits/{id}`. Точное значение ID заранее неизвестно: клиент использует ID из ответа и заголовка.

Пробелы вокруг имени сохраняй. Повторяющиеся имена допустимы: ID различает записи. Ответ `400` возвращается при отсутствующих или некорректных полях, пустом теле или повреждённом JSON; при ошибке новая строка не появляется. Формат тела ошибки свободный.

`GET /guild/recruits/{id}` возвращает `200` с актуальными значениями из SQL-таблицы. Если числовой `long` ID не найден, верни `404` без тела. Если путь не преобразуется в `long` или число выходит за его диапазон, Spring MVC вернёт `400`.

Каждый `GET` читает таблицу заново и не меняет её. Если строка изменена через другое соединение, API возвращает её новые поля.

**Что проверяешь ты:** ограничения входного DTO, запись ровно одной строки, получение ключа базы, содержимое `Location` и JSON-ответа. `@Valid` запускает знакомую Bean Validation; SQL-параметры связывает `JdbcTemplate`; Spring MVC преобразует путь в `long`.

## Примеры

1. `POST` с `{"name":"Лира","level":25}` → `201`; например, ответ может содержать `{"id":1,"name":"Лира","level":25}` и `Location: /guild/recruits/1`. ID в примере иллюстративный.
2. Следующий `GET /guild/recruits/1` → `200` с данными созданной записи.
3. `POST` с `{"name":" ","level":0}` → `400`; таблица остаётся без новой строки.

## Критерии готовности и файлы

- Все исходники приложения созданы тобой в `src/main/java/learning/task070/`; конфигурация и SQL — в `src/main/resources/`.
- `INSERT` параметризован и не указывает `id`. `JdbcTemplate` через `GeneratedKeyHolder` получает ID от БД; Java не присваивает и не вычисляет ID.
- В таблице `id` — identity и первичный ключ; начальных записей нет.
- Автоматические проверки размещены в `src/test/java/learning/task070/RecruitGeneratedIdApiTest.java`. Они проверяют схему и пустой старт, создание, сгенерированный ID, ответ и последующий `GET`, границы полей, ошибки, поведение при добавлении строк через другое соединение и чтение обновлений из SQL.
- Параметризованный SQL, передача `KeyHolder` и конструкторное внедрение дополнительно проверяются при просмотре кода.

## Проверка

Из корня проекта выполни:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

В IntelliJ IDEA выбери Temurin 21 в Project SDK и Maven Runner. Пока своё приложение не создано, появится ожидаемая ошибка `Unable to find a @SpringBootConfiguration`. Создай код и настройки сам; заготовок приложения в проекте нет.

После реализации напиши **«Решил»**.

## Результат проверки

Решение проверено 2026-10-04: `clean test` — 35 тестов, ошибок и падений нет. Подтверждены самостоятельное создание приложения, SQL `IDENTITY` и получение фактического `BIGINT` ID через `GeneratedKeyHolder`.
