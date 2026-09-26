# Задача №52 — Ответ на приглашение через HTTP

**Сложность:** средняя, закрепление без новой темы.

**Закрепляем:** HTTP `POST`, JSON boolean, Jackson, `Optional`, проверку ответа и статусов.

**Новое:** нет. Это ещё одна комбинация уже изученных приёмов HTTP и JSON.

Игрок принимает или отклоняет приглашение. API возвращает не только логическое значение, но и текстовый статус; оба поля должны согласовываться с отправленным ответом.

## Что сделать

В пакете `learning.task052` нужны два файла:

- `InvitationReply.java` — `public record InvitationReply(int invitationId, boolean accepted, String status) {}` без дополнительной логики.
- `InvitationReplyClient.java`:

```java
public InvitationReplyClient(URI baseUri)
public Optional<InvitationReply> reply(int invitationId, boolean accepted)
        throws IOException, InterruptedException
```

- Базовый URI должен быть абсолютным, со схемой `http` или `https`, хостом и путём, оканчивающимся на `/`. Иначе `IllegalArgumentException`.
- Если `invitationId <= 0`, выбрось `IllegalArgumentException` **до** сетевого запроса.
- Отправь `POST` на `baseUri.resolve("invitations/" + invitationId + "/reply")`. Тело — JSON-объект ровно с одним полем `accepted` типа JSON boolean (`true`/`false`, не строка). Используй Jackson и UTF-8. Заголовки: `Accept: application/json`, `Content-Type: application/json; charset=UTF-8`.
- При `200` разбери ответ в UTF-8 как JSON-объект. Обязательные поля: `invitationId` — положительный целый `int`, равный аргументу; `accepted` — JSON boolean, равный отправленному; `status` — JSON-строка, ровно `"принято"` для `true` или `"отклонено"` для `false`. Лишние поля игнорируй. Верни `Optional.of(new InvitationReply(...))`.
- При `404` верни `Optional.empty()`, не разбирая тело. При других статусах — `IllegalStateException` с кодом в сообщении, тоже без разбора тела.
- При `200` с неверным JSON, корнем или обязательными полями выбрось `IllegalStateException`. Ошибку синтаксиса JSON от Jackson сохрани как `cause`. Сетевые `IOException` и `InterruptedException` пробрасывай.

## Примеры

- `reply(12, true)`, ответ `200` с `{"invitationId":12,"accepted":true,"status":"принято"}` → `Optional.of(new InvitationReply(12, true, "принято"))`.
- `reply(12, false)`, ответ `200` с `{"invitationId":12,"accepted":false,"status":"отклонено"}` → `Optional.of(new InvitationReply(12, false, "отклонено"))`.
- `reply(12, true)`, ответ `200` с `"accepted":"true"` → `IllegalStateException`; ответ `404` с произвольным текстом → `Optional.empty()`.

## Ограничения и готовность

- Используй Java `HttpClient` и уже подключённый Jackson; новых зависимостей нет.
- Не составляй JSON конкатенацией строк. Не изменяй текст статуса в ответе.
- Тесты проверяют оба значения boolean, HTTP-запрос, UTF-8, статусы, неверные типы и поля, границы и сетевой сбой.

## Файлы

- Решение: `src/main/java/learning/task052/InvitationReply.java`, `src/main/java/learning/task052/InvitationReplyClient.java`.
- Тесты: `src/test/java/learning/task052/InvitationReplyClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
