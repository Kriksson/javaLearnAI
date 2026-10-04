# Задача №68 — Рекрут по ID через Spring JDBC

**Сложность:** базовая, небольшой шаг к работе Spring Boot с базой.

**Формат:** проектирование и сборка с нуля. Ты создаёшь файлы приложения и подключаешь зависимости сам. Имена классов и методов выбираешь ты; ниже указаны необходимые роли и внешний контракт.

**Закрепи:** `@RestController`, `@GetMapping`, `@PathVariable`, конструкторное внедрение, `Optional`, SQL `SELECT` и чтение `ResultSet`.

**Новое:** `JdbcTemplate` для чтения данных и запуск `schema.sql` при старте Spring Boot.

## Небольшой проект

Гильдия уже внесла рекрутов в таблицу. Сделай один endpoint:

```text
GET /guild/recruits/{id}
```

Он читает рекрута из базы и возвращает его в JSON. Если записи нет, клиент получает `404` — по этому ID рекрут не найден.

Используй H2 в памяти: отдельный сервер базы устанавливать не нужно. После завершения процесса данные исчезают; при следующем запуске SQL-скрипт создаёт таблицу и начальные записи заново.

Достаточно четырёх Java-файлов: стартовый класс, модель ответа, репозиторий и контроллер. Репозиторий передаётся в контроллер через конструктор.

## Материал: один запрос через `JdbcTemplate`

Ты уже работал с `PreparedStatement` и `ResultSet`. `JdbcTemplate` выполняет JDBC-вызовы и освобождает ресурсы. Ты задаёшь SQL, параметры и лямбду, которая превращает строку результата в Java-значение. Эту лямбду называют `RowMapper`. [Spring Framework: JDBC](https://docs.spring.io/spring-framework/reference/6.2/data-access/jdbc/core.html).

Ниже отдельный работающий пример про количество книг на складе. Для него нужны `spring-boot-starter-web`, `spring-boot-starter-jdbc` и H2. Пример остаётся в материале; своё приложение ты создаёшь сам.

`demo/DemoApplication.java`:

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

`@SpringBootApplication` включает настройку приложения и поиск компонентов в `demo`. `run(...)` создаёт Spring-контекст и запускает HTTP-сервер.

`src/main/resources/application.properties` демонстрационного проекта:

```properties
spring.datasource.url=jdbc:h2:mem:book-demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.sql.init.mode=always
```

URL выбирает базу H2 в памяти. Spring Boot создаёт `DataSource` — источник соединений — и `JdbcTemplate`, который можно получить через конструктор. Параметры H2 сохраняют базу между соединениями и передают управление завершением приложению. [Spring Boot: SQL](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html).

`src/main/resources/schema.sql` демонстрационного проекта:

```sql
CREATE TABLE book_stock (
    code VARCHAR(20) PRIMARY KEY,
    quantity INTEGER
);
INSERT INTO book_stock (code, quantity) VALUES ('atlas', 4);
```

`spring.sql.init.mode=always` включает выполнение `schema.sql` при запуске: первая команда создаёт таблицу, вторая добавляет книгу с четырьмя экземплярами. [Инициализация базы](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html).

`demo/BookStockRepository.java`:

```java
package demo;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookStockRepository {
    private final JdbcTemplate jdbc;

    public BookStockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Integer> quantity(String code) {
        return jdbc.query(
                "SELECT quantity FROM book_stock WHERE code = ?",
                (rows, rowNumber) -> rows.getInt("quantity"),
                code).stream().findFirst();
    }
}
```

- Конструктор получает готовый `JdbcTemplate` из Spring.
- `code` передаётся вместо `?`; значение не склеивается с текстом SQL.
- Лямбда читает `quantity` из текущей строки. `rowNumber` — номер строки, здесь он не нужен.
- `query(...)` возвращает список. `findFirst()` даёт `Optional` с числом или пустой результат, если строки нет.

`demo/BookStockController.java`:

```java
package demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookStockController {
    private final BookStockRepository repository;

    public BookStockController(BookStockRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/demo/stock/{code}")
    public ResponseEntity<Integer> quantity(@PathVariable("code") String code) {
        return repository.quantity(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

`@PathVariable` получает код книги из URL. Непустой `Optional` превращается в `200` с числом, пустой — в `404` без тела.

Путь данных: `GET /demo/stock/atlas` → Spring извлекает строку `atlas` → репозиторий передаёт её в SQL-параметр → H2 возвращает `quantity=4` → лямбда создаёт `Integer` → Spring отправляет `200` с JSON-числом `4`. Запусти `DemoApplication.main()` и проверь `curl -i http://localhost:8080/demo/stock/atlas`.

В задаче вместо одного числа ты прочитаешь три колонки и создашь модель рекрута. Spring преобразует ID из URL в `long`, а модель ответа — в JSON.

## Сделай по шагам

1. **Запусти приложение.** В существующем `pom.xml` сам подключи `spring-boot-starter-jdbc` версии из свойства `spring.boot.version` и `com.h2database:h2:2.3.232` с областью `runtime`. Создай стартовый класс с `main` и `@SpringBootApplication` в пакете `learning.task068`. Имеющиеся зависимости и Java 21 сохрани.
2. **Подготовь базу.** Создай `src/main/resources/application.properties`: настрой H2 в памяти и выполнение SQL-скрипта. В `src/main/resources/schema.sql` сам напиши `CREATE TABLE` и три `INSERT` по таблицам ниже.
3. **Прочитай одну запись.** Создай отдельную модель с `id`, `name`, `level` и `@Repository`, который получает `JdbcTemplate` через конструктор. Напиши один параметризованный `SELECT` по ID. Прочитай поля через `getLong`, `getString`, `getInt`. Для отсутствующей строки удобно вернуть `Optional.empty()`.
4. **Добавь endpoint.** Создай `@RestController`, получающий репозиторий через конструктор. Свяжи ID из пути с поиском и выбери ответ `200` или `404`.

Имена внутренних Java-классов и методов свободные. Имена таблицы и колонок нужны тестам и фиксированы.

## Таблица и начальные данные

Таблица `guild_recruits`:

| Колонка | Тип |
|---|---|
| `id` | `BIGINT PRIMARY KEY` |
| `name` | `VARCHAR(30)` |
| `level` | `INTEGER` |

После запуска в таблице должны быть ровно эти три записи:

| id | name | level |
|---:|---|---:|
| 10 | Лира | 25 |
| 20 | Кирилл | 1 |
| 30 | Мира | 100 |

ID начальных записей укажи непосредственно в SQL-скрипте. Это уже существующие рекруты, которых API только читает.

## HTTP-контракт

| Вход | Результат |
|---|---|
| ID найден | `200`, JSON с числовыми `id`, `level` и строковым `name` из таблицы |
| ID не найден, но помещается в `long`, включая `0` и отрицательные значения | `404` без тела |
| ID не является целым числом или выходит за диапазон `long` | `400`; формат тела не фиксирован |

Каждый запрос читает актуальную строку из базы; компоненты приложения не хранят собственную копию каталога. Поля возвращаются без изменения. `GET` не изменяет таблицу. Тест может вставить, изменить или удалить строку через другое соединение, чтобы проверить чтение данных.

**Твоя проверка в коде:** определить, нашлась ли строка, и вернуть `404` при её отсутствии. Преобразование ID в `long` и ответ `400` при ошибке преобразования выполняет Spring MVC. Проверять имя и уровень входящего JSON здесь не требуется: у endpoint нет тела запроса.

## Примеры

1. `GET /guild/recruits/10` → `200` и `{"id":10,"name":"Лира","level":25}`.
2. `GET /guild/recruits/999` → `404` без тела.
3. `GET /guild/recruits/abc` → `400`.

## Критерии готовности и файлы

- Приложение создано тобой с нуля; минимальные роли — стартовый класс, модель ответа, репозиторий и контроллер. Каждый самостоятельный класс находится в отдельном файле в `src/main/java/learning/task068/`.
- Ресурсы созданы тобой в `src/main/resources/`: `application.properties` и `schema.sql`.
- Репозиторий использует внедрённый `JdbcTemplate`, один `SELECT` с параметром `?` и чтение трёх колонок. Запрос не склеивает ID с SQL.
- Автоматические тесты в `src/test/java/learning/task068/GuildRosterApiTest.java` проверяют начальные данные, найденные и отсутствующие ID, ошибки преобразования, большие ID типа `long`, актуальность данных и отсутствие изменений после `GET`.
- Все тесты проходят. Параметризованный SQL и конструкторное внедрение дополнительно проверяются при просмотре кода.

## Проверка

В текущем окружении используй установленный полный JDK 21; в `pom.xml` остаётся `<maven.compiler.release>21</maven.compiler.release>`:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Для запуска из IntelliJ IDEA выбери этот JDK 21 в Project SDK и настройках запуска. Spring Boot 3.5.16 поддерживает Java 17–25, поэтому JDK 27 для проверки не используем. [Требования Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

Для сборки JAR замени `clean test` на `clean package`. Приложение можно запускать через собственный `main` в IntelliJ IDEA.

Пока стартовый класс не создан, ошибка `Unable to find a @SpringBootConfiguration` ожидаема. Когда класс появится, тесты смогут проверить настройку базы и endpoint.

После реализации напиши **«Решил»**. Запись новых рекрутов через API станет отдельным следующим шагом.

## Результат проверки

Задача решена и подтверждена 4 октября 2026 года: `clean test` на Temurin 21.0.12.1 — **16 тестов, 0 failures, 0 errors**. Дополнительно проверены параметризованный SQL, конструкторное внедрение, схема `VARCHAR(30)` и согласованные версии Spring Boot/JDBC 3.5.16 и H2 2.3.232.

Решение перенесено в этот каталог без изменения кода: `GuildRecruitsApplication.java`, `Recruit.java`, `GuildRecruitsRepository.java`, `GuildRecruitsController.java`, `application.properties`, `schema.sql` и `GuildRosterApiTest.java`. Рабочие каталоги `src` освобождены для следующей задачи.
