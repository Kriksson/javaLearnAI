# Задача №56 — Расчёт очков через Spring Boot

**Сложность:** средняя.

**Закрепляем:** `record`, проверки аргументов, HTTP-статусы и типизированный JSON-ответ Spring MVC.

**Впервые:** `@RequestBody` — Spring читает JSON-тело входящего запроса и превращает его в Java-объект; `@PostMapping` сопоставляет метод с `POST`-маршрутом. Вручную вызывать `ObjectMapper.readTree` в контроллере не нужно.

## Материал для изучения

В №55 Spring сам превращал `PlayerView` в JSON **ответа**. Теперь обратное направление: клиент присылает JSON, Spring создаёт из него `record` **запроса** и передаёт в параметр метода.

Отдельный код-пример, не связанный с очками:

```java
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EchoController {
    @PostMapping("/echo")
    EchoResponse echo(@RequestBody EchoRequest request) {
        return new EchoResponse(request.text());
    }
}

record EchoRequest(String text) {}
record EchoResponse(String text) {}
```

Для `POST /echo` с телом `{"text":"Привет"}` Spring создаст `new EchoRequest("Привет")`, вызовет `echo` и вернёт JSON `{"text":"Привет"}`. `@PostMapping` выбирает маршрут, `@RequestBody` включает преобразование входящего JSON, `@RestController` — преобразование результата в тело ответа. Невалидный по синтаксису JSON Spring отклоняет статусом `400` до вызова метода. Но **смысловые** правила — диапазоны чисел, обязательные поля — ты проверяешь в методе сам.

В этой задаче поля входного `record` имеют тип `Integer`, а не `int`: если поле не передано или равно JSON `null`, оно станет Java `null`, и его можно явно отклонить. Это не `@Valid`: автоматическую Bean Validation изучим отдельно позже.

## Что сделать

В пакете `learning.task056`:

- `ScoreApplication.java` — готовая точка входа Spring Boot; не меняй.
- `ScorePreviewRequest.java` — готовый `public record ScorePreviewRequest(Integer wins, Integer losses) {}`.
- `ScorePreview.java` — готовый `public record ScorePreview(int points, boolean bonus) {}`.
- `ScorePreviewController.java` — реализуй:

```java
public ResponseEntity<ScorePreview> preview(@RequestBody ScorePreviewRequest request)
```

- Пометь класс `@RestController`, а метод — `@PostMapping("/scores/preview")`. Используй существующий параметр `@RequestBody`.
- `wins` и `losses` должны быть не `null` и каждый от `0` до `100` включительно. Хотя бы одно число должно быть больше `0`. Нарушение → `400` без тела.
- За победу начисляй `3` очка, за поражение снимай `1`; базовый результат не может стать отрицательным: `max(0, wins * 3 - losses)`.
- При `wins >= 5` добавь `10` бонусных очков и установи `bonus = true`; иначе `bonus = false`.
- Верни `200` и `ScorePreview(points, bonus)`. Spring сам сериализует `record` в JSON с полями `points` (число) и `bonus` (boolean).
- Неверный синтаксис JSON, отсутствие тела и неподходящий JSON для `record` Spring должен обработать как `400`. Метод контроллера не должен запускаться для `GET`.
- Не используй ручной разбор JSON и `HttpExchange`; новых зависимостей нет.

## Примеры

- `POST /scores/preview`, `{"wins":3,"losses":1}` → `200`, `{"points":8,"bonus":false}`.
- `POST /scores/preview`, `{"wins":5,"losses":0}` → `200`, `{"points":25,"bonus":true}`.
- `POST /scores/preview`, `{"wins":0,"losses":1}` → `200`, `{"points":0,"bonus":false}`; `{"wins":0,"losses":0}` → `400`.

## Ограничения и готовность

- Используй только уже подключённый Spring Boot 3.5 и Java 21.
- Тесты проверяют обычные расчёты, порог бонуса, границы, пустые и неверные поля, ошибки JSON, маршрут и метод.

## Файлы

- Решение: `src/main/java/learning/task056/ScorePreviewController.java`.
- Готовые файлы: `src/main/java/learning/task056/ScoreApplication.java`, `ScorePreviewRequest.java`, `ScorePreview.java`.
- Тесты: `src/test/java/learning/task056/ScorePreviewControllerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```

Справка Spring: [`@RequestBody`](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-controller/ann-methods/requestbody.html).
