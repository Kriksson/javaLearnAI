# Задача №47 — Профиль игрока: HTTP + JSON

**Сложность:** средняя, закрепление без новых тем.

**Закрепляем:** `HttpClient` и `GET`, статусы HTTP, `Optional`, Jackson `JsonNode`, проверку JSON и `record`.

**Новое:** нет. После этой задачи будет ещё практика на HTTP и JSON без перехода к Spring Boot.

Получай профиль игрока у API и возвращай не строку JSON, а готовый `PlayerProfile`. Это соединяет задачи №44 и №45; создавать сервер не нужно — тесты поднимут локальный.

## Краткое повторение

Последовательность действий важна:

1. Проверь аргументы, собери URI и отправь `GET` с `Accept: application/json`.
2. Сначала посмотри `statusCode()`: `404` означает отсутствие игрока, другой статус кроме `200` — ошибку. Тело ошибки может быть обычным текстом, поэтому JSON разбирай **только при `200`**.
3. Для `200` проверь корень JSON и поля через `JsonNode`: `id` — положительный `int`, `name` — непустая JSON-строка, `level` — целое 1–100. После проверок создай `record` и заверни его в `Optional`.

Напоминание: `path("id")` вернёт missing-узел для отсутствующего поля; `isIntegralNumber()` отличает число от строки и дроби; `canConvertToInt()` защищает от переполнения. Некорректный JSON вызывает `JsonProcessingException` — здесь передай его как `cause` у `IllegalStateException`.

## Что сделать

Рабочие файлы в пакете `learning.task047`:

- `PlayerProfile.java` — `public record PlayerProfile(int id, String name, int level) {}`. Только данные, без дополнительной логики.
- `PlayerProfileClient.java` — реализуй:

```java
public PlayerProfileClient(URI baseUri)
public Optional<PlayerProfile> find(int playerId) throws IOException, InterruptedException
```

- Конструктор принимает абсолютный базовый URI со схемой `http`/`https`, хостом и путём, который заканчивается на `/`. Для `null` и неподходящего URI — `IllegalArgumentException`.
- `playerId <= 0` — `IllegalArgumentException` до отправки запроса.
- Отправь `GET` на `baseUri.resolve("players/" + playerId)` с `Accept: application/json`; читай тело как UTF-8.
- `200` → разбери JSON-объект и верни `Optional.of(new PlayerProfile(...))`. Поля `id`, `name`, `level` обязательны: `id > 0` и в диапазоне `int`; `name` — не пустая и не пробельная JSON-строка, сохраняй её без изменений; `level` — целое от 1 до 100. Лишние поля игнорируй.
- `404` → `Optional.empty()`; любой другой статус → `IllegalStateException` с кодом статуса в сообщении. Не пытайся разбирать тело при этих статусах.
- При статусе `200` неверный JSON или неверные поля → `IllegalStateException`; синтаксическую ошибку JSON сохрани как `cause`. Сетевые `IOException` и `InterruptedException` не перехватывай.

## Примеры

Базовый URI: `http://localhost:8080/api/`.

- `find(7)`, ответ `200` и `{"id":7,"name":"Лис","level":12}` → `Optional.of(new PlayerProfile(7, "Лис", 12))`.
- `find(999)`, ответ `404` с телом `Not found` → `Optional.empty()`.
- `find(7)`, ответ `503` с телом `Service unavailable` → `IllegalStateException` с `503` в сообщении.

## Ограничения и готовность

- Используй стандартный `HttpClient` и уже имеющийся Jackson; новых зависимостей нет.
- Не разбирай JSON вручную и не меняй полученное имя.
- Тесты проверяют запрос, обычный результат, границы, пропущенные и неверные поля, не-JSON тела ошибок, неверные аргументы и сетевой сбой.

## Файлы

- Решение: `src/main/java/learning/task047/PlayerProfile.java` и `src/main/java/learning/task047/PlayerProfileClient.java`.
- Автотесты: `src/test/java/learning/task047/PlayerProfileClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
