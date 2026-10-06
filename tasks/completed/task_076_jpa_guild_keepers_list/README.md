# Задача №76 — Хранители одной гильдии

**Сложность:** небольшая проектная, на уровне предыдущей задачи.

**Закрепи:** ручную сборку Spring Boot приложения, JPA-сущности, `@ManyToOne`, `@JoinColumn`, `JpaRepository`, списки, DTO и REST `GET`.

**Новое понятие:** производный метод Spring Data — запрос по имени метода, включая свойство связанной сущности и порядок результатов.

## Сценарий

В интерфейсе игры нужна страница состава выбранной гильдии. Верни только её хранителей. Существующая гильдия без участников должна открываться с пустым списком; неизвестный ID означает, что страницы гильдии нет, и требует `404`.

Самостоятельно создай приложение в пакете `learning.task076`. Код, конфигурацию и разбиение на файлы спроектируй с нуля. Архив №75 можно читать как справочник. Имена внутренних классов, полей и методов выбираешь ты; тесты проверяют таблицы, HTTP-контракт и свойства JPA-модели.

## Новый приём: запрос по имени метода

Spring Data создаёт реализацию метода репозитория по его имени. Название описывает **Java-поля сущности**, а не SQL-колонки. Например, ищем книги одного автора, упорядоченные по ID. Это отдельный учебный пример, его файлы не нужны в решении.

`Author.java`:

```java
package demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Author {
    @Id
    private Long id;

    protected Author() {}
    public Author(Long id) { this.id = id; }
    public Long getId() { return id; }
}
```

`Book.java`:

```java
package demo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Book {
    @Id
    private Long id;
    private String title;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    protected Book() {}
    public Book(Long id, String title, Author author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Author getAuthor() { return author; }
}
```

`BookRepository.java`:

```java
package demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthor_IdOrderByIdAsc(long authorId);
}
```

- `JpaRepository<Book, Long>` указывает сущность и тип её ключа; реализацию интерфейса создаёт Spring Data.
- `findBy` начинает условие поиска.
- `Author_Id` означает путь `book.author.id`. Символ `_` явно отделяет поле связи от поля внутри связанного объекта.
- `OrderByIdAsc` задаёт возрастание ID книги, а `List<Book>` возвращает все подходящие книги. Если совпадений нет, получится пустой список.
- `@JoinColumn(name = "author_id")` задаёт имя в БД. В названии метода используется `Author_Id`, соответствующее Java-полям `author` и `id`.

Например, после сохранения автора `7` и книг `12` и `4` вызов `repository.findByAuthor_IdOrderByIdAsc(7)` вернёт книги в порядке `4`, `12`. Книги других авторов не попадут в список. Чтобы проверить пример в отдельном Spring Boot приложении, зарегистрируй эти сущности и репозиторий, сохрани объекты через `save` и вызови метод.

Полный путь данных: запрос `GET /authors/7/books` → Spring преобразует `7` из URL в `long`/`Long` → код проверяет существование автора через репозиторий → вызывает производный метод → Spring Data строит запрос с условием по `author.id` и сортировкой → Hibernate читает БД → код преобразует книги в DTO → клиент получает JSON-массив. SQL вручную и тело метода репозитория писать не нужно. Механизм описан в [документации Spring Data](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html).

В задании ты пишешь проверки существования гильдии и преобразование выбранных хранителей в ответ. Пустой список хранителей сам по себе не доказывает отсутствие гильдии: сначала проверь родительскую запись.

## Что нужно собрать

Самостоятельно создай стартовый класс Spring Boot, две отдельные JPA-сущности, репозитории, контроллер и формат элемента ответа. Сервис для простого чтения необязателен. У сущности хранителя должна быть обязательная связь `@ManyToOne` с гильдией через `@JoinColumn`; обратную коллекцию `@OneToMany` пока не добавляй.

Используй существующее Maven-окружение: Spring Web, Spring Data JPA и H2 уже подключены. Дополнительных зависимостей не требуется; `maven.compiler.release` остаётся `21`.

Вручную создай `src/main/resources/application.properties`: отдельная in-memory H2, пользователь `sa`, пустой пароль, `spring.jpa.hibernate.ddl-auto=create-drop`. Hibernate создаёт схему; начальные записи и SQL-схему не добавляй. Таблицы при старте пусты.

| Таблица | Колонки |
|---|---|
| `guilds` | `id BIGINT` — identity primary key; `name VARCHAR(40) NOT NULL` |
| `guild_keepers` | `id BIGINT` — identity primary key; `name VARCHAR(40) NOT NULL`; `level INTEGER NOT NULL`; `guild_id BIGINT NOT NULL` — внешний ключ на `guilds.id` |

Выбирай хранителей через производный метод `JpaRepository`, фильтрующий по ID связанной гильдии и сортирующий по ID хранителя по возрастанию. Имя метода зависит от выбранных Java-полей. Не используй `findAll()` с последующей фильтрацией в Java, `@Query`, ручной SQL или `JdbcTemplate` в приложении: задача закрепляет запрос по имени метода. Объявленный метод должен использоваться при обработке запроса; это также проверяется при просмотре решения.

## HTTP-контракт

```text
GET /guilds/{guildId}/keepers
```

- Гильдия существует → `200 OK`, `Content-Type: application/json` и массив её хранителей, отсортированный по `id` по возрастанию.
- Каждый элемент содержит ровно `id`, `name`, `level`. Вложенная гильдия в этом ответе не нужна: она уже выбрана через URL.
- Гильдия существует, но хранителей нет → `200 OK` и `[]`.
- Гильдии нет → `404 Not Found`; формат тела ошибки свободный.
- ID хранителя и гильдии поддерживают значения больше `Integer.MAX_VALUE`; имена длиной 40 символов, уровни `0` и `Integer.MAX_VALUE` должны возвращаться без искажений.
- GET читает актуальные данные при каждом запросе и не изменяет строки обеих таблиц. После изменения принадлежности хранителя в БД следующий запрос должен видеть его в новой гильдии.
- База отклоняет запись хранителя без гильдии или со ссылкой на отсутствующую гильдию.

Примеры:

1. В гильдии `3` есть хранители `12` и `4`, а хранитель `8` относится к другой гильдии. `GET /guilds/3/keepers` → `200` и `[{"id":4,"name":"Mira","level":2},{"id":12,"name":"Rin","level":5}]`.
2. Гильдия `7` существует без участников → `200` и `[]`.
3. Гильдия `99` отсутствует → `404`, даже если в других гильдиях есть хранители.

## Критерии готовности и файлы

- Файлы приложения ты создаёшь в `src/main/java/learning/task076/`; каждый самостоятельный класс находится в отдельном файле.
- Конфигурацию ты создаёшь в `src/main/resources/application.properties`.
- Hibernate создаёт пустые таблицы с указанными типами, длинами, `NOT NULL`, identity-ключами и внешним ключом.
- Репозиторий объявляет и использует производный метод с фильтром по связи и возрастающим порядком ID; обратной `@OneToMany` нет.
- API выполняет все перечисленные правила, включая границы значений, изоляцию гильдий и чтение актуальных данных.
- Тесты тренера: `src/test/java/learning/task076/JpaGuildKeepersListApiTest.java`. Они проверяют схему и конфигурацию, наличие производного метода без привязки к именам внутренних классов, HTTP-контракт, ошибки, границы и отсутствие изменений при GET.

До создания приложения ожидаема ошибка отсутствующей Spring Boot конфигурации. Это начальное состояние; задача будет засчитана только после успешной проверки решения.

Запускай из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

После реализации должны пройти **5 тестов** без ошибок.
