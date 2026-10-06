# Задача №75 — Хранитель в гильдии через JPA-связь

**Сложность:** небольшая проектная.

**Закрепи:** самостоятельную сборку Spring Boot приложения, JPA-сущности, `JpaRepository`, REST `GET`, H2 и MockMvc.

**Новое понятие:** связь «многие к одному» через `@ManyToOne` и `@JoinColumn`.

## Сценарий

Каждый хранитель состоит в одной гильдии, а в одной гильдии может быть много хранителей. Создай приложение, которое находит хранителя по ID и возвращает его вместе с данными гильдии. Связь нужна, чтобы клиент видел принадлежность хранителя к гильдии, а база не хранила произвольный ID без существующей родительской записи.

## Новый приём: связь `@ManyToOne`

Например, каждый разведчик относится к одному походу, а в походе может быть много разведчиков:

`Expedition.java`:

```java
package demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expeditions")
public class Expedition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    protected Expedition() {}

    public Expedition(String code) { this.code = code; }
    public Long getId() { return id; }
    public String getCode() { return code; }
}
```

`Scout.java` в том же пакете:

```java
package demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "scouts")
public class Scout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @ManyToOne
    @JoinColumn(name = "expedition_id", nullable = false)
    private Expedition expedition;

    protected Scout() {}

    public Scout(String name, Expedition expedition) {
        this.name = name;
        this.expedition = expedition;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Expedition getExpedition() { return expedition; }
}
```

- `@ManyToOne` говорит JPA, что у многих разведчиков может быть один и тот же поход.
- `@JoinColumn` помещает ссылку в таблицу `scouts` под именем `expedition_id`; `nullable = false` делает участие обязательным.
- Поле `expedition` содержит связанный объект, а не число с ID.
- Когда Hibernate загружает разведчика, JPA связывает значение `scouts.expedition_id` со строкой похода.

Полный путь данных для отдельного примера: `GET /expeditions/scouts/12` → Spring превращает `12` в `Long` → репозиторий ищет `Scout` → Hibernate читает `scouts` и по `expedition_id` загружает `Expedition` → контроллер отдаёт JSON с вложенным объектом похода. В этой задаче проверь тот же путь для хранителя и гильдии, ответ `404` для неизвестного ID и отказ базы сохранить ссылку на несуществующую гильдию.

Пример использует разведчиков и походы; он не является заготовкой решения.

## Что нужно собрать

Выполни задачу в пакете `learning.task075`. Самостоятельно спроектируй Spring Boot приложение, две JPA-сущности (гильдия и хранитель), Spring Data JPA репозиторий и HTTP-контроллер. Имена внутренних классов и методов выбери сам. Отдельный сервис для простого поиска не обязателен.

В проекте уже подключены Spring Web, Spring Data JPA и H2. Создай `src/main/resources/application.properties` с отдельной in-memory базой H2, пользователем `sa`, пустым паролем и `spring.jpa.hibernate.ddl-auto=create-drop`. Hibernate должен создавать и удалять схему; начальные записи не добавляй.

Таблицы имеют контракт:

| Таблица | Колонки |
|---|---|
| `guilds` | `id BIGINT` — identity primary key; `name VARCHAR(40) NOT NULL` |
| `guild_keepers` | `id BIGINT` — identity primary key; `name VARCHAR(40) NOT NULL`; `level INTEGER NOT NULL`; `guild_id BIGINT NOT NULL` — внешний ключ на `guilds.id` |

Используй `@ManyToOne` и явную колонку `guild_id` для связи хранителя с гильдией. Каждая запись хранителя обязана ссылаться на существующую гильдию. Не добавляй коллекцию хранителей в сущность гильдии: двустороннюю связь и `@OneToMany` пока не изучаем.

## HTTP-контракт

```text
GET /guild/keepers/{id}
```

- Для существующего хранителя верни `200 OK` и JSON с полями `id`, `name`, `level` и вложенной `guild` с полями `id` и `name`.
- Для неизвестного ID верни `404 Not Found`.
- GET не изменяет строки ни в одной таблице.

Примеры:

1. Хранитель `Mira`, уровень `4`, состоит в гильдии `Silver Hawks`: ответ содержит `{"id":12,"name":"Mira","level":4,"guild":{"id":3,"name":"Silver Hawks"}}`.
2. Хранитель с ID `99` отсутствует → `404`.
3. Попытка сохранить хранителя с `guild_id`, которого нет в `guilds`, отклоняется внешним ключом БД и не создаёт строки.

## Критерии готовности

- Приложение и конфигурация вручную созданы в `src/main/java/learning/task075/` и `src/main/resources/application.properties`.
- Hibernate создаёт две таблицы с указанными колонками, ограничениями и внешним ключом; таблицы изначально пусты.
- JPA-модель использует связь `@ManyToOne`; простой поиск выполняется через `JpaRepository`.
- API отдаёт хранителя с правильной гильдией, возвращает `404` для неизвестного ID и не изменяет данные.
- Граница длины имени в 40 символов и целостность внешнего ключа проверяются тестами.
- Тесты тренера лежат в `src/test/java/learning/task075/JpaKeeperGuildRelationApiTest.java` и не зависят от имён внутренних классов.

До сборки приложения тесты могут завершиться ошибкой запуска Spring-контекста — это ожидаемо. После реализации запусти из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Успешный результат должен показать 3 теста без ошибок.
