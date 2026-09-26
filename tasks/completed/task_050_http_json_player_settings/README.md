# Задача №50 — Настройки игрока через HTTP

**Сложность:** средняя, закрепление без новой темы.

**Закрепляем:** HTTP `GET`, статусы ответа, `Optional`, Jackson `JsonNode`, проверку типов JSON и `record`.

**Новое:** нет. JSON-значение `true`/`false` здесь используется так же, как уже знакомые строка и число.

Получи настройки игрока из игрового API. В отличие от предыдущего ответа, один из параметров теперь логический: JSON `true` — не строка `"true"` и не число `1`.

## Краткое повторение

- `HttpRequest.newBuilder(baseUri.resolve("players/" + playerId + "/settings")).GET()` создаёт запрос; добавь `Accept: application/json`.
- При `200` разбирай тело через `ObjectMapper.readTree`. Для логического поля используй `isBoolean()` до чтения значения; `path(...)` для отсутствующего поля не пройдёт проверку типа.
- При `404` верни пустой результат, при других статусах — ошибку с HTTP-кодом. JSON разбирай только при `200`: тело ошибок может быть обычным текстом.
- Читай тело именно как UTF-8 и сохраняй синтаксическую ошибку Jackson как `cause` у `IllegalStateException`.

## Что сделать

Создай два файла в пакете `learning.task050`:

- `PlayerSettings.java` — `public record PlayerSettings(int playerId, boolean notificationsEnabled, String theme) {}` без дополнительной логики.
- `PlayerSettingsClient.java` — реализуй:

```java
public PlayerSettingsClient(URI baseUri)
public Optional<PlayerSettings> find(int playerId) throws IOException, InterruptedException
```

- Базовый URI должен быть абсолютным, со схемой `http`/`https`, хостом и путём, который заканчивается на `/`; иначе `IllegalArgumentException`.
- `playerId <= 0` — `IllegalArgumentException` до запроса.
- Отправь `GET` на `baseUri.resolve("players/" + playerId + "/settings")` с `Accept: application/json`; прочитай ответ в UTF-8.
- Для `200` тело должно быть JSON-объектом с обязательными полями: `playerId` — положительный целый `int`, равный запрошенному ID; `notificationsEnabled` — именно JSON boolean; `theme` — не пустая и не пробельная JSON-строка. Сохрани тему без изменений, игнорируй лишние поля и верни `Optional.of(new PlayerSettings(...))`.
- Для `404` верни `Optional.empty()` без разбора тела. Для любого другого статуса выбрось `IllegalStateException` с кодом статуса в сообщении, тоже без разбора тела.
- Для `200` с неверным JSON, корнем или полями выбрось `IllegalStateException`; если Jackson не смог разобрать JSON, сохрани исключение как `cause`. Сетевые `IOException` и `InterruptedException` не перехватывай.

## Примеры

- `find(7)`, ответ `200`, `{"playerId":7,"notificationsEnabled":true,"theme":"dark"}` → `Optional.of(new PlayerSettings(7, true, "dark"))`.
- `find(7)`, ответ `200` с `"notificationsEnabled":"true"` → `IllegalStateException`: это строка, а не boolean.
- `find(999)`, ответ `404` и текст `Not found` → `Optional.empty()`.

## Ограничения и готовность

- Используй только уже подключённые Java `HttpClient` и Jackson; новых зависимостей нет.
- Не разбирай JSON вручную и не меняй полученное значение `theme`.
- Тесты проверяют запрос, UTF-8, статусы, границы, неверные типы и поля, несовпадение ID и сетевой сбой.

## Файлы

- Решение: `src/main/java/learning/task050/PlayerSettings.java` и `src/main/java/learning/task050/PlayerSettingsClient.java`.
- Автотесты: `src/test/java/learning/task050/PlayerSettingsClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
