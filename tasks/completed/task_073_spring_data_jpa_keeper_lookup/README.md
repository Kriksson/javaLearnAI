# Задача №73 — Найти хранителя через Spring Data JPA

**Сложность:** небольшая проектная.

**Закрепи:** Spring Boot, HTTP `GET`, H2, JSON и интеграционные тесты через MockMvc.

**Новые понятия:** JPA-сущность и репозиторий Spring Data JPA.

## Сценарий

Гильдия хранит список хранителей в базе. Создай небольшое приложение, которое по ID читает хранителя из H2 и возвращает его через REST API. В этой задаче приложение только читает данные: строки для проверки добавляют тесты.

## Новый приём: JPA-сущность и Spring Data репозиторий

JPA описывает, как Java-объект связан с таблицей. Hibernate — реализация JPA, которая выполняет SQL и создаёт Java-объект из выбранной строки. Spring Data JPA по интерфейсу репозитория создаёт реализацию поиска.

Пример на отдельной модели ящика припасов:

```java
package demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "supply_crates")
public class SupplyCrate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label", nullable = false, length = 40)
    private String label;

    protected SupplyCrate() {
        // Hibernate создаёт объект через конструктор без аргументов.
    }

    public SupplyCrate(String label) {
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
```

```java
package demo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyCrateRepository extends JpaRepository<SupplyCrate, Long> {
}
```

- `@Entity` включает класс в модель JPA, а `@Table` задаёт таблицу.
- `@Id` отмечает ключ строки; `@GeneratedValue` поручает базе выдать его при вставке.
- `@Column` задаёт имя, обязательность и длину колонки.
- `JpaRepository<SupplyCrate, Long>` даёт готовый `findById`, поэтому отдельный SQL-метод для простого поиска писать не нужно.
- Публичный конструктор и геттеры оставлены для кода приложения и JSON; Hibernate использует защищённый конструктор без аргументов.

Полный путь данных для отдельного примера: запрос `GET /storage/crates/41` → Spring MVC превращает `41` в `Long` → код вызывает `repository.findById(41)` → Hibernate читает строку `supply_crates` и заполняет поля `SupplyCrate` → контроллер возвращает объект → Jackson выдаёт JSON вроде `{"id":41,"label":"North stores"}`. В этой задаче тесты проверят чтение правильной строки, ответ `404` для отсутствующего ID и то, что `GET` не меняет данные.

Этот пример показывает приём на другой таблице и не является заготовкой решения.

## Что нужно собрать

Выполни работу в пакете `learning.task073`. Самостоятельно создай стартовый класс Spring Boot, JPA-сущность для хранителя, репозиторий и HTTP-контроллер. Для простого чтения контроллер может обращаться к репозиторию напрямую; добавляй сервис, только если он нужен выбранной логике.

Подключи в `pom.xml` зависимость Spring Boot `spring-boot-starter-data-jpa` той же версии, что используется остальными Spring Boot зависимостями проекта (`3.5.16`). H2 уже подключён. Сам создай `application.properties`: задай `spring.datasource.url` со значением вида `jdbc:h2:mem:<имя>;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, пользователя `sa`, пустой пароль и `spring.jpa.hibernate.ddl-auto=create-drop`. Таблица должна начинать работу пустой; не добавляй стартовые записи.

Модель и таблица имеют контракт:

| Java-данные | Таблица `guild_keepers` |
|---|---|
| генерируемый `Long id` | `id BIGINT` — первичный ключ и identity |
| `String name` | `name VARCHAR(40) NOT NULL` |
| `int level` | `level INTEGER NOT NULL` |

Настрой явное имя таблицы и колонок. Используй JPA-аннотации для отображения модели; ручные `schema.sql`, SQL-запросы в рабочем коде и `JdbcTemplate` для поиска в этой задаче не нужны.

## HTTP-контракт

```text
GET /guild/keepers/{id}
```

- Если хранитель с таким ID есть, верни `200 OK` и JSON с числовыми полями `id`, `level` и текстовым полем `name`.
- Если такого ID нет, верни `404 Not Found`.
- Если ID в пути нельзя преобразовать в `long` (например, текст или число за пределами диапазона `long`), верни `400 Bad Request`.
- `GET` не должен изменять строки таблицы.

Примеры:

1. В таблице есть `{id: 1, name: "Mira", level: 4}`. `GET /guild/keepers/1` → `200` и `{"id":1,"name":"Mira","level":4}`.
2. В таблице нет строки с ID `99`. `GET /guild/keepers/99` → `404`.
3. `GET /guild/keepers/not-a-number` → `400`.

## Критерии готовности

- Код приложения самостоятельно создан в `src/main/java/learning/task073/`; тесты не требуют заданных имён внутренних классов и методов.
- У приложения есть JPA-сущность, таблица соответствует контракту и содержит ноль начальных строк.
- Spring Data JPA предоставляет репозиторий, через который endpoint читает запись.
- Endpoint возвращает точные данные найденной строки, `404` для неизвестного ID и `400` для нечислового или слишком большого ID.
- Тесты проверяют JPA-настройку и таблицу, обычный поиск, пустую таблицу, неизвестные и граничные ID, ошибочное значение пути, а также отсутствие изменения строк после `GET`.
- Схема, настройки и решение создаются вручную. Не добавляй готовые классы приложения или `TODO`-заготовки.

Рабочие файлы создавай в `src/main/java/learning/task073/`, настройки — в `src/main/resources/application.properties`. Автоматические тесты тренера уже размещены в `src/test/java/learning/task073/JpaGuildKeepersApiTest.java`.

До реализации и добавления зависимости тесты могут падать из-за отсутствующей конфигурации JPA или класса Spring Boot. Это ожидаемо; задача засчитывается только после успешной проверки.

Запусти все проверки из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

После реализации напиши **«Решил»**.
