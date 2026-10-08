# Задача №81 — Страница хранителей без N+1

**Сложность: базовая. Один новый приём поверх знакомой пагинации.**

Для общего списка хранителей нужно показывать название гильдии каждого из них. Хранители одной страницы могут принадлежать разным гильдиям. Верни страницу с этими данными, не выполняя отдельный запрос для каждой гильдии.

Самостоятельно спроектируй и собери приложение с нуля в пакете `learning.task081`. Имена Java-классов и полей выбираешь сам. Тренер предоставляет условие и тесты; код, конфигурацию и сборку создаёшь и проверяешь ты.

Закрепляем: JPA-сущности, `@ManyToOne`, производный запрос с `GreaterThanEqual`, стабильную сортировку, `Pageable` / `PageRequest`, `Page`, DTO страницы и валидацию параметров URL.

Новый материал: **проблема N+1 и план загрузки связи через `@EntityGraph`**. Для понимания плана загрузки познакомимся с `FetchType.LAZY`.

## Откуда берётся N+1

Представь страницу из пяти хранителей разных гильдий. Один SQL-запрос загружает хранителей. При чтении данных их гильдий Hibernate может выполнить ещё пять запросов. Получается `1 + N` запросов, а для метаданных страницы иногда нужен ещё подсчёт. Чем больше разных связанных объектов, тем больше обращений к БД. Повторные ссылки на одну гильдию в одном persistence context могут использовать уже загруженный объект.

`@ManyToOne(fetch = FetchType.LAZY)` задаёт отложенную загрузку: связанную сущность обычно загружают, когда нужны её данные. Например, вызов `keeper.getGuild().getName()` может вызвать SQL. Само по себе `LAZY` не устраняет N+1. Обращение к незагруженной связи после закрытия persistence context может привести к `LazyInitializationException`.

`@EntityGraph(attributePaths = "имяJavaПоля")` на методе репозитория задаёт, какую связь загрузить вместе с результатом именно этого метода. Для нашей связи many-to-one Hibernate получает хранителей и гильдии общей выборкой. Чтение имени уже загруженной гильдии не требует отдельного запроса.

Транзакция помогает организовать работу с сущностями, но сама не сокращает число запросов. В этой задаче `spring.jpa.open-in-view=false`: данные для DTO должны быть загружены до сериализации ответа.

[Hibernate: стратегии загрузки и N+1](https://docs.hibernate.org/orm/6.6/userguide/html_single/#fetching), [Spring Data JPA: EntityGraph](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/query-methods.html#jpa.entity-graph).

### Отдельный работающий пример: книги и издатели

Пример компилируется в Spring Boot JPA приложении, где пакет `example.books` сканируется. Он остаётся в учебном материале. Каждую указанную сущность и компонент размещают в своём файле.

`Publisher.java`:

```java
package example.books;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    protected Publisher() {}

    public Publisher(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

`@Entity` регистрирует сущность, `@Id` и `IDENTITY` задают ID от базы. `name` хранит имя издателя; пустой конструктор нужен Hibernate.

`Book.java`:

```java
package example.books;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int pages;

    @ManyToOne(fetch = FetchType.LAZY)
    private Publisher publisher;

    protected Book() {}

    public Book(int pages, Publisher publisher) {
        this.pages = pages;
        this.publisher = publisher;
    }

    public Publisher getPublisher() {
        return publisher;
    }
}
```

Поле `pages` задаёт длину книги. `@ManyToOne(fetch = FetchType.LAZY)` описывает отложенно загружаемую связь с издателем. Getter позволяет обращаться к этой связи.

`BookRepository.java`:

```java
package example.books;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(attributePaths = "publisher")
    List<Book> findByPagesGreaterThanEqualOrderByIdAsc(int minimumPages);
}
```

Имя метода задаёт знакомые фильтр и сортировку. `attributePaths = "publisher"` указывает **Java-поле связи в Book**, а не название таблицы или колонки. Для этого метода издатель загружается вместе с книгой. Тот же приём применим к методу с `Pageable` и результатом `Page`.

`BookCatalog.java`:

```java
package example.books;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookCatalog {
    private final BookRepository repository;

    public BookCatalog(BookRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<String> publisherNames(int minimumPages) {
        if (minimumPages < 0) {
            throw new IllegalArgumentException("Invalid minimum pages");
        }
        return repository.findByPagesGreaterThanEqualOrderByIdAsc(minimumPages)
                .stream()
                .map(book -> book.getPublisher().getName())
                .toList();
    }
}
```

- Spring внедряет репозиторий через конструктор.
- Проверку отрицательного порога пишет разработчик.
- `@Transactional(readOnly = true)` задаёт транзакцию чтения для метода внедрённого сервиса.
- Производный метод получает книги с уже загруженными издателями благодаря графу.
- `map` читает имена издателей; после завершения метода остаётся обычный список строк.

Пусть книги с ID `1` и `2` имеют 300 и 220 страниц и издателей `North` и `East`, а книга `3` имеет 100 страниц. Вызов внедрённого `catalog.publisherNames(200)` возвращает `["North", "East"]`.

Полный путь для HTTP API: строки URL → Spring MVC преобразует их в числа → твой код проверяет диапазоны → создаёт `PageRequest` → репозиторий применяет фильтр, порядок, страницу и граф загрузки → Hibernate создаёт объекты со связанными данными → код читает их поля и собирает DTO → Jackson сериализует DTO. Spring отклоняет непреобразуемые числа; допустимые диапазоны проверяешь ты. Фильтра родителя в этой задаче нет: список общий для всех гильдий.

## Модель и настройка

Вручную создай `src/main/resources/application.properties`: URL начинается с `jdbc:h2:mem:`, пользователь `sa`, пустой пароль, `spring.jpa.hibernate.ddl-auto=create-drop`, **`spring.jpa.open-in-view=false`**. Hibernate создаёт пустые таблицы. Не добавляй начальные данные, `schema.sql` или `data.sql`.

Создай две отдельные сущности. Явно отобрази таблицы через `@Table`, колонки через `@Column`, связь через `@ManyToOne(fetch = FetchType.LAZY)` и `@JoinColumn(name = "guild_id", nullable = false)`. Обратную коллекцию `@OneToMany` не добавляй.

| Таблица | Колонки |
|---|---|
| `guilds` | `id BIGINT` — identity primary key; `name VARCHAR(40) NOT NULL` |
| `guild_keepers` | `id BIGINT` — identity primary key; `name VARCHAR(40) NOT NULL`; `level INTEGER NOT NULL`; `guild_id BIGINT NOT NULL` — внешний ключ на `guilds.id` |

В Spring Data JPA репозитории хранителей объяви производный метод с одним условием: уровень `GreaterThanEqual`. В имени задай порядок: уровень DESC, затем ID хранителя ASC. Метод принимает порог и `Pageable`, возвращает `Page` сущностей. Добавь к нему `@EntityGraph(attributePaths = ...)` с именем Java-поля связи с гильдией. Имена внутренних классов и полей выбираешь сам.

## HTTP-контракт

```http
GET /keepers?minLevel=5&page=0&size=2
```

- Все три параметра обязательны и имеют диапазон `int`: `minLevel >= 0`, `0 <= page <= 1000`, `1 <= size <= 5`.
- Отбирай хранителей **из всех гильдий** с `level >= minLevel`. Сортируй по уровню DESC и ID ASC, затем выбирай страницу. Нумерация с нуля.
- Успех → `200 OK`, `Content-Type: application/json`. Корневой DTO содержит ровно `content`, `page`, `size`, `totalElements`, `totalPages`.
- Каждый элемент `content` содержит ровно `id`, `name`, `level`, `guild`. Вложенный DTO `guild` содержит ровно `id` и `name` гильдии этого хранителя.
- `page` и `size` сохраняют запрошенные значения. `totalElements` имеет тип `long` и учитывает все совпадения во всех гильдиях. `totalPages` берётся из результата репозитория.
- Если совпадений нет, даже при наличии пустых гильдий, верни пустой `content` и нулевые общие количества. Здесь нет проверки существования конкретной гильдии и ответа `404`.
- Допустимая страница за последней → `200` с пустым содержимым и правильными общими количествами.
- Отсутствующий, пустой, состоящий из пробелов или непреобразуемый параметр, число вне диапазона `int` либо нарушение границ → `400`. Формат тела ошибки свободный.
- Поддерживай ID хранителя и гильдии больше `Integer.MAX_VALUE`, уровни `0` и `Integer.MAX_VALUE`, имена длиной 40 символов, кириллицу и кавычки.
- При каждом запросе возвращай актуальные имя гильдии, связь хранителя, его имя и уровень; обновляй общие количества при изменении фильтруемых данных.
- GET при любом исходе не изменяет строки. База отклоняет хранителя без гильдии или со ссылкой на несуществующую гильдию.

## Ограничение числа запросов

Каждый успешный GET должен выполнить **не больше двух SQL-запросов через Hibernate**: выборка страницы вместе с гильдиями и, при необходимости, общий подсчёт. Для пяти хранителей пяти разных гильдий этот предел остаётся тем же. Подготовка тестовых данных, запуск приложения и независимая проверка таблиц в этот счётчик не входят.

Тесты включают статистику Hibernate только для проверки; в конфигурацию приложения её добавлять не требуется. Тесты отключают Open Session in View, второй уровень кеша, кеш запросов и пакетную загрузку. Число запросов уменьшаем за счёт графа текущей выборки.

Не используй `@Query`, ручной SQL, `JdbcTemplate`, `findAll()`, отдельный поиск гильдии для каждого хранителя, ручную фильтрацию или пагинацию всех строк в Java, `PageImpl`, `@BatchSize` или настройки пакетной загрузки. Тесты используют JDBC для независимой подготовки и проверки БД. Преобразование содержимого страницы через `Page.map`, Stream API или цикл разрешено. JPA-сущности и `Page` напрямую в JSON не возвращай.

## Примеры

Пусть гильдии `1` и `2` называются `North` и `East`. Хранители: `(10, "Mira", 5, guild=1)`, `(20, "Rin", 7, guild=2)`, `(30, "Low", 4, guild=1)`.

1. `GET /keepers?minLevel=5&page=0&size=1` → `200`:

```json
{
  "content": [
    {"id": 20, "name": "Rin", "level": 7, "guild": {"id": 2, "name": "East"}}
  ],
  "page": 0,
  "size": 1,
  "totalElements": 2,
  "totalPages": 2
}
```

2. Тот же запрос с `page=1` возвращает хранителя `10` с гильдией `North`; общие количества остаются 2 и 2. С `page=2` содержимое пустое, но количества те же.
3. `minLevel=10&page=0&size=2` → `200`, пустой список и нулевые количества. `page=-1` или отсутствие `size` → `400`.

## Файлы и критерии готовности

- Вручную создавай рабочие файлы в `src/main/java/learning/task081/`.
- Роли: стартовый класс Spring Boot, две сущности, репозиторий хранителей, контроллер, DTO гильдии, DTO хранителя и DTO страницы. Сервис и второй репозиторий добавляй, если они нужны твоему проектированию. Каждый самостоятельный класс — отдельный файл.
- Конфигурацию вручную создавай в `src/main/resources/application.properties`. Существующий учебный Maven-проект допустим как окружение проверки; зависимости и сборку проверяешь самостоятельно. Исходный код остаётся Java 21.
- Тесты тренера: `src/test/java/learning/task081/JpaKeeperGuildPageApiTest.java`.
- Готовность: верная схема, LAZY-связь, производный метод с графом, правильные вложенные DTO и страницы, актуальные данные, `200`/`400`, не более двух SQL-запросов на успешный GET. Ограничения дополнительно проверяются при просмотре решения.

До создания приложения ожидаема ошибка отсутствующей Spring Boot конфигурации. После реализации должны пройти **8 интеграционных тестов** и предварительные проверки схемы, связи, конфигурации и метода репозитория. До успешной проверки задача не засчитывается.

Команда из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```
