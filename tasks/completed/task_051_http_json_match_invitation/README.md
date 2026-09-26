# Задача №51 — Приглашение игрока через HTTP

**Сложность:** средняя, закрепление без новой темы.

**Закрепляем:** HTTP `POST`, JSON-запрос и ответ, Jackson, `Optional`, проверку статусов и типов полей.

**Новое:** нет. Здесь соединяются уже знакомые отправка JSON и разбор JSON-ответа.

Игрок отправляет другому игроку приглашение в матч. Текст приглашения может содержать кавычки, обратную косую черту и кириллицу — поэтому формируй JSON через Jackson, а не склеивай строку вручную.

## Что сделать

Создай два файла в пакете `learning.task051`:

- `MatchInvitation.java` — `public record MatchInvitation(int invitationId, int senderId, int recipientId, String message) {}` без дополнительной логики.
- `MatchInvitationClient.java` — реализуй:

```java
public MatchInvitationClient(URI baseUri)
public Optional<MatchInvitation> send(int senderId, int recipientId, String message)
        throws IOException, InterruptedException
```

- Базовый URI: абсолютный, схема `http`/`https`, есть хост, путь заканчивается на `/`. Иначе `IllegalArgumentException`.
- До запроса проверь: оба ID положительны и различны; `message` не `null` и не пуст после `isBlank()`. Неверные аргументы — `IllegalArgumentException`. Исходный `message` не обрезай и не меняй.
- Отправь `POST` на `baseUri.resolve("invitations")`. JSON-объект запроса содержит ровно `senderId`, `recipientId`, `message` с соответствующими JSON-типами и исходным текстом. Кодируй тело UTF-8; заголовки `Accept: application/json` и `Content-Type: application/json; charset=UTF-8`.
- При `201` разбери UTF-8 тело как JSON-объект. Обязательные поля: `invitationId` — положительный `int`; `senderId` и `recipientId` — положительные целые `int`, равные аргументам; `message` — JSON-строка, равная отправленной. Лишние поля игнорируй. Верни `Optional.of(new MatchInvitation(...))`.
- При `409` верни `Optional.empty()` без разбора тела. При любом другом статусе выбрось `IllegalStateException` с HTTP-кодом в сообщении, тоже без разбора тела.
- При `201` с неверным JSON, корнем или полями выбрось `IllegalStateException`. Если Jackson не смог разобрать JSON, сохрани его исключение как `cause`. Сетевые `IOException` и `InterruptedException` пробрасывай без подмены.

## Примеры

- `send(7, 9, "Привет!")`, ответ `201` и `{"invitationId":3,"senderId":7,"recipientId":9,"message":"Привет!"}` → `Optional.of(new MatchInvitation(3, 7, 9, "Привет!"))`.
- `send(7, 9, "  бой  ")` отправляет и сохраняет пробелы вокруг текста.
- `send(7, 7, "Бой")` → `IllegalArgumentException` до HTTP-запроса; ответ `409` с произвольным текстом → `Optional.empty()`.

## Критерии готовности

- Используй Java `HttpClient` и уже подключённый Jackson; новых зависимостей нет.
- Не собирай JSON конкатенацией строк. Не разбирай JSON для `409` и прочих неуспешных статусов.
- Тесты проверяют запрос и UTF-8, экранирование текста, статусы, границы и ошибки JSON, неверные аргументы и сетевой сбой.

## Файлы

- Решение: `src/main/java/learning/task051/MatchInvitation.java` и `src/main/java/learning/task051/MatchInvitationClient.java`.
- Автотесты: `src/test/java/learning/task051/MatchInvitationClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
