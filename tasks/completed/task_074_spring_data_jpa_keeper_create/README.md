# Задача №74 — Создать хранителя через Spring Data JPA

**Сложность:** небольшая проектная.

**Закрепи:** ручную сборку Spring Boot приложения, REST `POST`, JSON, H2, MockMvc и JPA-сущность из предыдущей задачи.

**Новые понятия:** `JpaRepository.save` для новой сущности; ответ `201 Created` со ссылкой `Location`.

## Сценарий

Гильдия принимает новых хранителей. Создай с нуля приложение, которое принимает имя и уровень хранителя, сохраняет запись в H2 и возвращает созданный ресурс. ID создаёт база данных, поэтому клиент не передаёт его в запросе.

## Новый приём: сохранение сущности и `Location`

Вызов `save` сохраняет новую JPA-сущность. Если её ID ещё не задан, Hibernate отправляет `INSERT`, база выдаёт identity ID, а сохранённый объект содержит этот ID. В REST API `201 Created` сообщает об успешном создании; заголовок `Location` указывает адрес созданного ресурса.

Пример на отдельной модели ящика припасов:

```java
record NewCrate(String label) {}

@PostMapping("/storage/crates")
ResponseEntity<SupplyCrate> create(@RequestBody NewCrate body) {
    SupplyCrate crate = new SupplyCrate(body.label());
    SupplyCrate saved = crates.save(crate);
    URI location = URI.create("/storage/crates/" + saved.getId());
    return ResponseEntity.created(location).body(saved);
}
```

- Spring MVC и Jackson превращают JSON `{"label":"North stores"}` в `NewCrate`.
- Первая строка метода создаёт новую сущность без ID.
- `crates.save(crate)` сохраняет её; после `INSERT` объект `saved` содержит ID, выданный базой.
- `URI` собирает адрес новой записи.
- `ResponseEntity.created(location)` задаёт статус `201` и заголовок `Location`; `.body(saved)` отдаёт JSON с ID и полями сущности.

Полный путь данных: `POST /storage/crates` с JSON → Jackson создаёт `NewCrate` → приложение создаёт сущность → `JpaRepository.save` и Hibernate выполняют `INSERT` в H2 → база выдаёт ID → API возвращает `201`, заголовок `Location` и JSON ресурса. В этой задаче проверь таким же способом хранителя, а также граничную длину имени и некорректный JSON.

Это пример на другой модели; не копируй его как решение задачи.

## Что нужно собрать

Выполни задачу в пакете `learning.task074`. Самостоятельно создай стартовый класс Spring Boot, JPA-сущность, репозиторий и HTTP-контроллер. Внутренние имена классов и методов выбери сам. Для передачи входных данных можешь создать отдельный DTO/record. Отдельный сервис для одной операции не обязателен.

В проекте уже подключены Spring Web, Spring Data JPA и H2. Не добавляй повторные зависимости. Сам создай `src/main/resources/application.properties`: настрой in-memory URL H2, пользователя `sa`, пустой пароль и `spring.jpa.hibernate.ddl-auto=create-drop`. Hibernate должен создавать и удалять схему; начальных записей быть не должно.

Таблица имеет внешний контракт:

| Поле | Требование |
|---|---|
| `id` | `BIGINT`, первичный ключ, identity, не `NULL` |
| `name` | `VARCHAR(40) NOT NULL` |
| `level` | `INTEGER NOT NULL` |

Используй таблицу `guild_keepers` и соответствующие имена колонок `id`, `name`, `level`. ID в JSON тела запроса не нужен: приложение создаёт новую сущность без ID. Имя длиной 40 символов и любое значение Java `int`, включая `Integer.MAX_VALUE`, допустимы. Не добавляй ограничения, которых нет в этом условии.

## HTTP-контракт

```text
POST /guild/keepers
Content-Type: application/json
```

Тело запроса содержит `name` и `level`, например:

```json
{"name":"Mira","level":4}
```

При корректном JSON:

- сохрани ровно одну новую строку;
- верни `201 Created` и JSON созданного хранителя с числовыми полями `id`, `level` и строковым `name`;
- `id` должен совпадать с identity-значением в базе;
- верни заголовок `Location` с путём `/guild/keepers/{id}`, где `{id}` — тот же ID, что в JSON.

Если тело запроса не является корректным JSON, верни `400 Bad Request` и не добавляй строку.

Примеры:

1. `POST` с `{"name":"Mira","level":4}` → `201`, например `Location: /guild/keepers/1`, JSON `{"id":1,"name":"Mira","level":4}`; база содержит эту строку.
2. Имя из 40 символов и `level: 2147483647` → `201`; имя и число сохранены без изменений.
3. Тело `{"name":` → `400`; таблица остаётся пустой.

## Критерии готовности

- Приложение и конфигурация собраны вручную в `src/main/java/learning/task074/` и `src/main/resources/application.properties`.
- Hibernate создал таблицу с указанными колонками; в начале работы она пуста.
- Endpoint сохраняет новую сущность через Spring Data JPA, а не через ручной SQL или `JdbcTemplate`.
- Ответ содержит ID, реально созданный БД, и `Location` указывает на тот же ресурс.
- 40-символьное имя и граничное значение `int` сохраняются; некорректный JSON даёт `400` без записи.
- Внутренние классы и методы могут называться как угодно; обязательны только внешний HTTP-контракт и данные таблицы.

Тесты тренера находятся в `src/test/java/learning/task074/JpaGuildKeeperCreateApiTest.java`. Пока приложения нет, Spring-тесты ожидаемо завершатся ошибкой запуска контекста. После ручной сборки запускай из корня проекта:

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-21.0.12.1 PATH=/home/kriksson/.jdks/temurin-21.0.12.1/bin:$PATH MAVEN_USER_HOME="$PWD/.maven-home" bash ./mvnw -Dmaven.repo.local="$PWD/.maven-home/repository" clean test
```

Успешный результат должен показать 3 теста без ошибок.
