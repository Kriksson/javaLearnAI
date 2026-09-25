# Задача №46 — Регистрация игрока через HTTP API

**Сложность:** средняя+

**Закрепляем:** `HttpClient`, URI, статусы HTTP, Jackson `ObjectMapper`/`JsonNode`, валидацию и обработку ошибок.

**Новое:** HTTP `POST` и формирование JSON-тела запроса.

Теперь соедини навыки двух предыдущих задач: отправь имя и уровень игрока на сервер, а из ответа получи созданный ID. Тесты используют локальный HTTP-сервер, внешнее API не нужно.

## Материал для изучения

`GET` получает ресурс, а `POST` передаёт данные серверу для создания нового. В этом API клиент отправляет `POST /api/players` с JSON `{"name":"Лис","level":12}`. При успехе сервер отвечает статусом `201 Created` и JSON `{"id":42}`.

- `Content-Type: application/json; charset=UTF-8` описывает **тело запроса**.
- `Accept: application/json` сообщает, какой формат **ответа** ожидает клиент.
- Для `POST` в Java используй `HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)`; ответ можно прочитать через знакомый `BodyHandlers.ofString(StandardCharsets.UTF_8)`.

Не собирай JSON конкатенацией: имя может содержать кавычки, обратный слеш или перевод строки. Jackson экранирует их при сериализации. Для этого можно создать `ObjectNode` через `mapper.createObjectNode()`, заполнить поля `put(...)` и вызвать `mapper.writeValueAsString(node)`. Для ответа используй `readTree(...)` и уже знакомые проверки `JsonNode`.

Пример только для одного поля запроса:

```java
ObjectNode body = mapper.createObjectNode();
body.put("name", name);
```

## Что сделать

В `src/main/java/learning/task046/PlayerRegistrationClient.java` реализуй:

```java
public PlayerRegistrationClient(URI baseUri)
public int register(String name, int level) throws IOException, InterruptedException
```

- В конструкторе принимай абсолютный базовый URI со схемой `http`/`https`, хостом и путём, оканчивающимся на `/`. Для `null` или неподходящего URI выбрасывай `IllegalArgumentException`.
- До запроса проверь: `name` не `null` и не состоит только из пробельных символов; `level` от 1 до 100. Иначе выбрасывай `IllegalArgumentException`. Имя не обрезай и не меняй.
- Отправь `POST` на `baseUri.resolve("players")` с двумя заголовками выше и UTF-8 JSON-телом ровно с полями `name` и `level`.
- При статусе `201` извлеки из JSON-ответа `id`: положительное целое число, помещающееся в `int`. Остальные поля ответа игнорируй; верни ID.
- Для другого статуса выбрасывай `IllegalStateException` с HTTP-кодом в сообщении. Для некорректного ответа (`не JSON`, корень не объект, нет `id`, неправильный тип или значение) тоже выбрасывай `IllegalStateException`; синтаксическую ошибку JSON сохрани как `cause`.
- `IOException` сетевого запроса и `InterruptedException` не перехватывай. Не добавляй новые зависимости и не закрывай `HttpClient`.

## Примеры

Базовый URI: `http://localhost:8080/api/`.

- `register("Лис", 12)` → запрос `POST /api/players` с телом `{"name":"Лис","level":12}`; ответ `201`, `{"id":42}` → `42`.
- `register(" Лис ", 1)` сохраняет пробелы имени в JSON; ответ `201`, `{"id":1,"bonus":true}` → `1`.
- `register("Лис", 101)` → `IllegalArgumentException`, запрос не отправляется.
- Ответ `409` → `IllegalStateException` с `409` в сообщении.

## Ограничения и готовность

- Не собирай JSON вручную; используй Jackson для записи и чтения.
- Тесты проверяют метод, URI, заголовки, JSON и экранирование, границы, неверный ввод, статусы, повреждённые ответы и сетевой сбой.
- После решения запусти все тесты и сообщи «Готово».

## Файлы

- Решение: `src/main/java/learning/task046/PlayerRegistrationClient.java`.
- Автотесты: `src/test/java/learning/task046/PlayerRegistrationClientTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
