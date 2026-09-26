# Задача №53 — Первый серверный API игроков

**Сложность:** средняя; первый шаг на серверной стороне HTTP.

**Закрепляем:** HTTP-методы и статусы, JSON через Jackson, `Map`, проверку входных данных.

**Впервые:** `HttpHandler` и `HttpExchange` из JDK. Это учебный HTTP-сервер без Spring Boot и новых зависимостей: сначала разберём, что сервер делает с запросом, а затем перейдём к фреймворку.

## Материал для изучения

- Раньше `HttpClient` **посылал** запрос и читал ответ. Теперь твой класс реализует `HttpHandler`: сервер сам вызывает `handle(HttpExchange exchange)` при входящем запросе.
- `HttpExchange` содержит обе стороны обмена. `getRequestMethod()` и `getRequestURI().getPath()` дают метод и путь запроса; `getResponseHeaders()` задаёт заголовки ответа.
- Чтобы ответить, вызови `sendResponseHeaders(code, length)`, затем запиши байты в `getResponseBody()`. Для JSON сначала создай объект через Jackson, сериализуй и кодируй в UTF-8; `length` — **число байтов**, не символов строки.
- Для ответа без тела используй `sendResponseHeaders(code, -1)`. Поток ответа или сам `exchange` нужно закрыть даже при раннем выходе; удобно использовать `try/finally`.
- Путь `/players/7` задаёт ID ресурса. `GET` читает игрока, `200` означает успех, `404` — игрока нет, `400` — неверный ID/путь, `405` — неподдерживаемый метод. Сервер в тестах уже создан: тебе нужен только обработчик.

Мини-схема вызова: `HttpClient → HttpServer → PlayerProfileHandler.handle(exchange) → HTTP-ответ → HttpClient`.

### Небольшой пример: проверка здоровья сервера

Это **другой** обработчик: он отвечает обычным текстом `OK` на `GET /health`. Здесь нет игроков, разбора ID и JSON, но порядок действий такой же, как в задаче:

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // ответ без тела
                return;
            }

            byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }
}
```

Сервер связывает адрес и обработчик вызовом `server.createContext("/health", new HealthHandler())`; в №53 это **уже делает тест** для `/players/`. При запросе `GET /health` сервер вызывает `handle`:

1. `getRequestMethod()` читает метод запроса. Если это не `GET`, отправляется `405` без тела; `return` выходит из метода, но `finally` всё равно выполнится.
2. Для успешного ответа строка `OK` превращается в байты UTF-8. `Content-Type` сообщает клиенту формат, а `sendResponseHeaders(200, body.length)` отправляет статус и точную длину **байтов**.
3. `getResponseBody().write(body)` записывает тело только **после** отправки заголовков. `exchange.close()` завершает обмен при любом пути выполнения.

Для №53 вместо строки `OK` понадобится JSON, а перед ответом — чтение пути через `exchange.getRequestURI().getPath()` и поиск игрока.

## Что сделать

Реализуй `src/main/java/learning/task053/PlayerProfileHandler.java`:

```java
public class PlayerProfileHandler implements HttpHandler {
    public PlayerProfileHandler(Map<Integer, String> playerNames)
    public void handle(HttpExchange exchange) throws IOException
}
```

- Конструктор принимает карту `ID → имя`. Для `null` выбрось `IllegalArgumentException`. Имена в переданной карте гарантированно не `null`; не меняй карту, снимок/копию делать не требуется.
- Сначала проверь метод. Для любого метода кроме `GET` ответь `405` без тела и с заголовком `Allow: GET`, независимо от пути.
- Для `GET` разбери путь строго формата `/players/{id}`: `{id}` — одна или более цифр `0–9`, положительное число в диапазоне `int`. Ведущие нули допустимы (`/players/007` означает ID `7`). Отсутствие ID, лишние сегменты, буквы, знак минус, ноль и переполнение — `400` без тела.
- Если ID корректен, но отсутствует в карте, ответь `404` без тела.
- Если игрок найден, ответь `200` с заголовком `Content-Type: application/json; charset=UTF-8` и JSON-объектом ровно с полями `playerId` (JSON-число) и `name` (JSON-строка из карты без изменений). Сформируй JSON через Jackson и отправь UTF-8 байты.
- Закрывай ресурсы ответа для всех веток. Не запускай сервер в решении: тест сам регистрирует твой обработчик на `/players/`.

## Примеры

- Карта `{7 → "Кот"}`, `GET /players/7` → `200`, `{"playerId":7,"name":"Кот"}`.
- `GET /players/8` → `404` без тела; `GET /players/abc` → `400` без тела.
- `POST /players/7` → `405`, `Allow: GET`, без тела.

## Ограничения и готовность

- Используй `com.sun.net.httpserver.HttpHandler`/`HttpExchange` и уже подключённый Jackson; новых зависимостей нет.
- Не составляй JSON вручную. Тесты проверяют обычный ответ, UTF-8 и экранирование, границы ID, ошибки пути, статусы и заголовки.

## Файлы

- Решение: `src/main/java/learning/task053/PlayerProfileHandler.java`.
- Тесты: `src/test/java/learning/task053/PlayerProfileHandlerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
