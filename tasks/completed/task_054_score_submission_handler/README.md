# Задача №54 — Приём результата игрока через HTTP

**Сложность:** средняя; продолжение серверного HTTP API.

**Закрепляем:** `HttpHandler`, статусы и заголовки, Jackson, `Map`, проверку JSON-полей.

**Впервые:** чтение тела **входящего** запроса через `HttpExchange.getRequestBody()`. Раньше ты создавал тело на стороне `HttpClient`; теперь сервер должен прочитать присланные байты.

## Материал для изучения

`exchange.getRequestBody()` — поток байтов, которые прислал клиент. Например, для `POST` с телом `{"message":"Привет"}` можно сделать:

```java
byte[] requestBytes = exchange.getRequestBody().readAllBytes();
JsonNode root = new ObjectMapper().readTree(requestBytes);
String message = root.path("message").asText();
```

Первая строка получает байты запроса, вторая разбирает JSON через Jackson, третья читает поле. Это лишь пример чтения: в реальном обработчике **перед** извлечением значения проверь, что JSON — объект, поле существует и имеет нужный тип. Ошибку разбора или неверные поля преобразуй в HTTP `400`, а не в необработанное исключение.

Ниже отдельный работающий пример без JSON: обработчик считает количество **байтов** в произвольном `POST`-теле и отвечает обычным текстом. Он не решает задачу о рекордах.

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class BodyLengthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            byte[] input = exchange.getRequestBody().readAllBytes();
            byte[] output = String.valueOf(input.length).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, output.length);
            exchange.getResponseBody().write(output);
        } finally {
            exchange.close();
        }
    }
}
```

`getRequestBody()` читает **запрос**, а `getResponseBody()` пишет **ответ**. Между ними твой обработчик решает, какой статус и JSON вернуть.

## Что сделать

Реализуй `src/main/java/learning/task054/ScoreSubmissionHandler.java`:

```java
public class ScoreSubmissionHandler implements HttpHandler {
    public ScoreSubmissionHandler(Map<Integer, Integer> bestScores)
    public void handle(HttpExchange exchange) throws IOException
}
```

- Конструктор принимает **изменяемую** карту `playerId → лучший результат`. Для `null` выбрось `IllegalArgumentException`. Храни именно переданный объект карты: тест проверяет его после запроса. В карте гарантированно лежат положительные ID и результаты от `0` до `1000`.
- Сначала проверь метод: только точное `POST`. Иной метод → `405` без тела, заголовок ответа `Allow: POST`.
- Затем проверь точный путь `/scores`. Другой путь → `404` без тела. Тест регистрирует твой обработчик на `/scores` и сам запускает сервер.
- Прочитай тело запроса и разбери Jackson как JSON-объект. Обязательные поля: `playerId` — положительный целый `int`, `score` — целый `int` от `0` до `1000` включительно. Лишние поля игнорируй. Некорректный JSON, корень, типы или значения → `400` без тела. Не изменяй карту при ошибке.
- Если `playerId` отсутствует в карте → `404` без тела. Нового игрока не создавай.
- Если игрок найден, новый лучший результат — максимум старого и присланного. Обнови карту **только если** новый результат выше старого. Отправь `200` с `Content-Type: application/json; charset=UTF-8` и JSON-объектом ровно с полями `playerId` (число), `bestScore` (число), `improved` (boolean: `true` только при обновлении).
- Длину ответа считай в UTF-8 **байтах**. Закрывай `exchange` для всех веток. Не собирай JSON строковой конкатенацией.

## Примеры

- Карта `{7 → 100}`, `POST /scores` с `{"playerId":7,"score":120}` → `200`, `{"playerId":7,"bestScore":120,"improved":true}`; карта теперь содержит `120`.
- Карта `{7 → 100}`, `POST /scores` с `{"playerId":7,"score":80}` → `200`, `{"playerId":7,"bestScore":100,"improved":false}`; карта не меняется.
- `POST /scores` с `{"playerId":7,"score":"120"}` → `400`, а с ID отсутствующего игрока → `404`.

## Ограничения и готовность

- Только JDK `HttpServer` и уже подключённый Jackson; новых зависимостей нет.
- Тесты проверяют обновление и сохранение рекорда, границы значений, ошибки JSON, неизвестного игрока, путь, метод и заголовки.

## Файлы

- Решение: `src/main/java/learning/task054/ScoreSubmissionHandler.java`.
- Тесты: `src/test/java/learning/task054/ScoreSubmissionHandlerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
