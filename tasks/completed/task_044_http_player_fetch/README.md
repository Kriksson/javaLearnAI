# Задача №44 — Загрузка профиля игрока по HTTP

**Сложность:** средняя

**Закрепляем:** конструктор с проверкой аргументов, `Optional`, обработку ошибок и JUnit-тесты.

**Новое:** устройство HTTP-запроса и ответа; стандартный Java `HttpClient`.

Реализуй клиент, который запрашивает профиль игрока у игрового API. Пока возвращай JSON как обычную строку: разбор JSON будет отдельной следующей темой.

## Материал для изучения

HTTP-клиент отправляет **запрос** на URI, сервер возвращает **ответ**: статус, заголовки и тело. Метод `GET` получает ресурс, не изменяя его. Для `GET /api/players/7` ответы в этой задаче означают:

- `200 OK` — игрок найден; в теле лежит JSON, например `{"id":7,"name":"Лис"}`;
- `404 Not Found` — игрока нет;
- другой статус (например, `500`) — неожиданный ответ сервера.

Заголовок запроса `Accept: application/json` сообщает, какой формат ответа ожидает клиент. Он сам по себе **не разбирает** JSON.

В стандартной библиотеке Java есть `java.net.http.HttpClient`. Запрос собирается через `HttpRequest.newBuilder(uri).GET()`, синхронно отправляется через `client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))`. У ответа есть `statusCode()` и `body()`. Сетевой сбой может привести к `IOException`, прерывание потока — к `InterruptedException`; в этой задаче передавай их вызывающему коду. Чтобы получить адрес игрока от базового URI вида `http://host:port/api/`, используй `baseUri.resolve("players/" + playerId)`; завершающий `/` здесь важен.

## Что сделать

В `src/main/java/learning/task044/PlayerApiClient.java` реализуй:

```java
public PlayerApiClient(URI baseUri)
public Optional<String> fetchPlayerJson(int playerId) throws IOException, InterruptedException
```

- Конструктор принимает абсолютный базовый URI со схемой `http` или `https` и путём, заканчивающимся `/`. Для `null` или неподходящего URI выбрось `IllegalArgumentException`.
- Если `playerId <= 0`, выбрось `IllegalArgumentException` **до отправки запроса**.
- Отправь `GET` на `baseUri.resolve("players/" + playerId)` с заголовком `Accept: application/json`.
- При статусе `200` верни `Optional.of(body)` без изменения тела; при `404` — `Optional.empty()`; при любом другом статусе — `IllegalStateException` с кодом статуса в сообщении.
- Не перехватывай `IOException` и `InterruptedException` от `send()`. Не разбирай JSON и не добавляй сторонних библиотек.

## Примеры

Базовый URI: `http://localhost:8080/api/`.

- `fetchPlayerJson(7)`, ответ `200`, тело `{"id":7,"name":"Лис"}` → `Optional` с точно этой строкой.
- `fetchPlayerJson(999)`, ответ `404` → `Optional.empty()`.
- `fetchPlayerJson(7)`, ответ `500` → `IllegalStateException` с `500` в сообщении.

## Ограничения и готовность

- Используй стандартный `HttpClient`; в тестах работает локальный HTTP-сервер, интернет не нужен.
- Тесты проверяют путь и метод запроса, заголовок `Accept`, UTF-8, статусы `200`/`404`/`201`/`500`, сетевой сбой, неверные ID и базовые URI.
- Пока не добавляй классы модели игрока: на этом шаге важно понять границу между HTTP и JSON.

## Файлы

- Решение: `src/main/java/learning/task044/PlayerApiClient.java`.
- Автотесты: `src/test/java/learning/task044/PlayerApiClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
