# Задача №49 — Получение результата матча

**Сложность:** средняя, закрепление без новых тем.

**Закрепляем:** HTTP `GET`, `Optional`, статусы ответа, разбор и валидацию JSON через Jackson.

**Новое:** нет. Задача продолжает историю отправки результата из №48: теперь нужно получать сохранённую запись.

## Краткое повторение

Клиент отправляет `GET /api/scores/{entryId}` с `Accept: application/json`. После ответа сначала обработай статус: `200` — разбирай JSON, `404` — верни `Optional.empty()`, остальные — ошибка с кодом статуса. На `404` и `503` сервер может прислать обычный текст, поэтому до проверки статуса не вызывай `readTree`.

Jackson `readTree` возвращает `JsonNode`. Для обязательных полей полезен `path(...)`: отсутствующее поле не даёт Java `null`, но не проходит проверку типа. Для числа последовательно проверь `isIntegralNumber()`, `canConvertToInt()` и допустимый диапазон. Ошибку синтаксиса JSON заверни в `IllegalStateException`, сохранив исходное исключение как `cause`.

## Что сделать

Размести два самостоятельных файла в пакете `learning.task049`:

- `MatchResult.java` — `public record MatchResult(int entryId, String matchName, int points) {}` без дополнительной логики.
- `MatchResultClient.java` — реализуй:

```java
public MatchResultClient(URI baseUri)
public Optional<MatchResult> find(int entryId) throws IOException, InterruptedException
```

- Базовый URI должен быть абсолютным, со схемой `http`/`https`, хостом и путём, оканчивающимся `/`. Иначе — `IllegalArgumentException`.
- Для `entryId <= 0` выбрось `IllegalArgumentException` до запроса.
- Отправь `GET` на `baseUri.resolve("scores/" + entryId)` с `Accept: application/json`; прочитай тело как UTF-8.
- При `200` тело должно быть JSON-объектом с обязательными полями: `entryId` — положительный целый `int`, **равный запрошенному ID**; `matchName` — не пустая и не пробельная JSON-строка (не меняй её); `points` — целое число от `0` до `Integer.MAX_VALUE`. Лишние поля игнорируй и верни `Optional.of(new MatchResult(...))`.
- При `404` верни `Optional.empty()` без разбора тела. Для любого другого статуса — `IllegalStateException` с HTTP-кодом в сообщении, тоже без разбора тела.
- При `200` неверный JSON, корень или поля — `IllegalStateException`. Если парсер Jackson выбросил `JsonProcessingException`, передай его как `cause`. Сетевые `IOException` и `InterruptedException` не перехватывай.

## Примеры

- `find(18)`, ответ `200`, `{"entryId":18,"matchName":"Финал","points":120}` → `Optional.of(new MatchResult(18, "Финал", 120))`.
- `find(18)`, ответ `404` и текст `Not found` → `Optional.empty()`.
- `find(18)`, ответ `200` с `{"entryId":19,"matchName":"Финал","points":120}` → `IllegalStateException`: сервер вернул не ту запись.

## Ограничения и готовность

- Используй только уже подключённые `HttpClient` и Jackson; новых зависимостей нет.
- Не разбирай JSON вручную и не переписывай тесты.
- Тесты покрывают запрос, статусы, обычные и граничные данные, неверные поля, несовпадение ID и сетевой сбой.

## Файлы

- Решение: `src/main/java/learning/task049/MatchResult.java` и `src/main/java/learning/task049/MatchResultClient.java`.
- Автотесты: `src/test/java/learning/task049/MatchResultClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
