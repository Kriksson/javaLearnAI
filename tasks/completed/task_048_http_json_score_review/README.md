# Задача №48 — Отправка результата матча

**Сложность:** средняя, закрепление без новых тем.

**Закрепляем:** HTTP `POST`, заголовки и статусы, сериализацию и разбор JSON через Jackson, `Optional`, валидацию.

**Новое:** нет. Это ещё одна практика на HTTP и JSON, как ты просил.

Клиент отправляет результат матча игровому API. Сервер либо создаёт запись и возвращает её ID, либо сообщает, что такой результат уже есть.

## Краткое повторение

- Адрес запроса и его тело — разные части: URI передаётся в `HttpRequest.newBuilder(uri)`, JSON-тело — в `POST(BodyPublishers.ofString(...))`.
- `Content-Type` описывает отправляемый JSON, `Accept` — желаемый формат ответа.
- Собирай JSON через `ObjectNode`, а не конкатенацией: название матча может содержать кавычки и перенос строки.
- Сначала обработай HTTP-статус. Ответы `409` и `503` могут содержать обычный текст; разбирать JSON нужно только при `201`.
- При разборе `entryId` различай JSON-число и строку, дробь и целое, проверь диапазон `int` и положительность.

## Что сделать

В `src/main/java/learning/task048/ScoreSubmissionClient.java` реализуй:

```java
public ScoreSubmissionClient(URI baseUri)
public Optional<Integer> submit(int playerId, String matchName, int points)
        throws IOException, InterruptedException
```

- В конструкторе принимай абсолютный базовый URI со схемой `http`/`https`, хостом и путём, заканчивающимся `/`. Иначе выбрасывай `IllegalArgumentException`.
- До отправки проверь: `playerId > 0`, `matchName` не `null` и не пробельная строка, `points >= 0`. Иначе — `IllegalArgumentException`, без запроса.
- Отправь `POST` на `baseUri.resolve("scores")` с заголовками `Accept: application/json` и `Content-Type: application/json; charset=UTF-8`. UTF-8 тело должно быть JSON-объектом **ровно** с полями `playerId`, `matchName`, `points`; название сохраняй без изменений.
- Ответ `201` содержит JSON-объект с обязательным `entryId` — положительным целым числом в диапазоне `int`. Лишние поля игнорируй; верни `Optional.of(entryId)`.
- Ответ `409` означает уже существующий результат: верни `Optional.empty()`, не разбирая тело. Любой другой статус — `IllegalStateException` с кодом статуса в сообщении, также без разбора тела.
- При `201` неверный JSON, корень или `entryId` — `IllegalStateException`; если Jackson не смог разобрать JSON, сохрани исходную ошибку как `cause`. Сетевые `IOException` и `InterruptedException` не перехватывай.

## Примеры

- `submit(7, "Финал", 120)` → `POST /api/scores`, тело `{"playerId":7,"matchName":"Финал","points":120}`; ответ `201` с `{"entryId":42}` → `Optional.of(42)`.
- `submit(7, "Финал", 0)`, ответ `409` с текстом `Already exists` → `Optional.empty()`.
- Ответ `503` с текстом `Unavailable` → `IllegalStateException` с `503` в сообщении.

## Ограничения и готовность

- Используй стандартный Java `HttpClient` и уже подключённый Jackson; новых зависимостей нет.
- Не собирай JSON вручную. Не добавляй лишние классы или слои.
- Тесты проверяют запрос, границы, экранирование строки, `201`/`409`/другие статусы, неверные ответы и сетевой сбой.

## Файлы

- Решение: `src/main/java/learning/task048/ScoreSubmissionClient.java`.
- Автотесты: `src/test/java/learning/task048/ScoreSubmissionClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
