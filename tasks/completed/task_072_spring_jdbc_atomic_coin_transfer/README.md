# Задача №72 — Атомарный перевод монеты между сундуками

**Сложность:** небольшая проектная. Два SQL-изменения в одном сценарии.

**Формат:** собери приложение с нуля. Сам создай классы, пакеты, конфигурацию и SQL-схему в `learning.task072`; используй текущий Maven-проект и уже подключённые зависимости.

**Закрепи:** Spring MVC, Bean Validation, слои контроллера/сервиса/репозитория, параметризованный `JdbcTemplate.update`, H2 и интеграционные тесты.

**Новое:** Spring `@Transactional` и откат нескольких SQL-изменений при ошибке.

## Сценарий

Гильдия перемещает одну монету из одного сундука в другой. Сначала приложение списывает монету из источника, затем зачисляет её получателю. Эти изменения должны пройти вместе: если получатель отсутствует, списание из источника откатывается.

Самостоятельно создай таблицу `guild_chests`:

```sql
CREATE TABLE guild_chests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    coins BIGINT NOT NULL CHECK (coins >= 0)
);
```

Настрой H2 в памяти и запуск `schema.sql` при старте Spring Boot. Начальный набор таблицы пустой. Для проверки тесты сами добавляют сундуки через JDBC.

## Новый приём: `@Transactional`

В отдельном приложении склада резервирование уменьшает свободный остаток и записывает бронь. Оба шага размещены в одном методе Spring-сервиса:

Таблица для этого отдельного примера:

```sql
CREATE TABLE shelf_bins (
    id BIGINT PRIMARY KEY,
    available INTEGER NOT NULL CHECK (available >= 0),
    reserved INTEGER NOT NULL CHECK (reserved BETWEEN 0 AND 10)
);

INSERT INTO shelf_bins (id, available, reserved) VALUES (1, 3, 10);
```

```java
package demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShelfRepository {
    private final JdbcTemplate jdbc;

    public ShelfRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int takeOne(long shelfId) {
        return jdbc.update(
                "UPDATE shelf_bins SET available = available - 1 WHERE id = ? AND available > 0",
                shelfId
        );
    }

    public int reserveOne(long shelfId) {
        return jdbc.update(
                "UPDATE shelf_bins SET reserved = reserved + 1 WHERE id = ? AND reserved < 10",
                shelfId
        );
    }
}
```

```java
package demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShelfReservationService {
    private final ShelfRepository repository;

    public ShelfReservationService(ShelfRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void reserve(long shelfId) {
        if (repository.takeOne(shelfId) == 0) {
            throw new IllegalStateException("No item to reserve");
        }
        if (repository.reserveOne(shelfId) == 0) {
            throw new IllegalStateException("Reservation was not recorded");
        }
    }
}
```

Контроллер передаёт валидированный запрос в Spring-сервис. Когда Spring вызывает публичный метод управляемого `@Service`-объекта, он начинает транзакцию. Репозиторий выполняет оба `UPDATE` в той же транзакции. Нормальный выход из метода сохраняет изменения; неперехваченное runtime-исключение приводит к откату обоих запросов. Если поймать исключение внутри метода и просто вернуть результат, Spring увидит нормальный выход и может зафиксировать изменения.

В демонстрационной таблице у полки `available=3` и `reserved=10`. Первый запрос уменьшает `available` до 2, второй не находит строку для обновления и возвращает 0. Runtime-исключение выходит из `reserve`, поэтому после отката значения снова равны `3` и `10`.

В примере запрос `POST` → Spring создаёт DTO из JSON → `@Valid` проверяет поля → контроллер вызывает `reserve` → сервис запускает две SQL-операции → ошибка второго шага выходит из метода → первая операция откатывается. Тесты склада здесь не нужны: в этой задаче ты проверишь перевод монет, включая неизменившийся баланс источника при отсутствующем получателе.

Пример иллюстрирует транзакционный приём на другом сценарии и не задаёт имена классов для решения.

## HTTP-контракт

```text
POST /guild/coin-transfers
Content-Type: application/json
```

Тело содержит `fromId` и `toId` — положительные числа типа `long`.

- Если оба ID существуют, различаются и в исходном сундуке есть хотя бы одна монета, уменьши `coins` источника ровно на 1 и увеличь `coins` получателя ровно на 1. Верни `204 No Content` без тела.
- Если источник отсутствует или в нём нет монеты, верни `409 Conflict`; таблица не меняется.
- Если получатель отсутствует, верни `404 Not Found`. Даже если SQL уже успел списать монету с источника, его баланс после ответа должен остаться прежним.
- Если ID одинаковы, не выполняй перевод и верни `400 Bad Request`.
- При пустом, повреждённом или невалидном JSON, отсутствующем поле, неположительном ID или числе за пределами `long` верни `400 Bad Request`. Данные не меняются.

Примеры:

1. Сундук 1 содержит 3 монеты, сундук 2 — 8. Запрос `{"fromId":1,"toId":2}` → `204`; после него балансы равны 2 и 9.
2. Источник содержит 2 монеты, получателя с ID 99 нет. Тот же запрос на перевод к 99 → `404`; у источника по-прежнему 2 монеты.
3. Источник содержит 0 монет, получатель — 5. Запрос перевода → `409`; балансы остаются 0 и 5.

## Критерии готовности

- Контроллер принимает JSON, валидирует его и возвращает статусы по контракту.
- Репозиторий использует параметризованные SQL-запросы. Списание выполняется только при `coins >= 1`.
- Перевод выполняется внутри транзакционного метода Spring-сервиса. Для отсутствующего получателя метод выбрасывает runtime-исключение, чтобы Spring откатил уже выполненное списание; HTTP-слой преобразует эту ситуацию в `404`.
- Повреждённый запрос, отсутствующие ID, нулевой баланс, одинаковые ID и ошибки перевода не оставляют частичных изменений.
- Автотесты размещены в `src/test/java/learning/task072/CoinTransferApiTest.java`; они проверяют схему, обычный перевод, границы и ошибочные сценарии, включая фактический откат.
- Самостоятельно создай стартовый класс, модель запроса, репозиторий, сервис и контроллер в `src/main/java/learning/task072/`, а настройки и схему — в `src/main/resources/`. Имена внутренних классов и методов выбирай сам.

До создания приложения тесты могут завершиться ожидаемой ошибкой `Unable to find a @SpringBootConfiguration`. Код приложения и конфигурация не подготовлены — создай их вручную.

Запусти все проверки из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

После реализации напиши **«Решил»**.
