# Задача №62 — Единый ответ об ошибках Spring Boot

**Сложность:** средняя.

**Закрепляем:** `@Valid`, Bean Validation, `ResponseEntity`, JSON-ответы и коллекции.

**Впервые:** `@RestControllerAdvice` и `@ExceptionHandler` — обработка исключений нескольких контроллеров в отдельном классе.

## Материал для изучения

В №61 Spring сам возвращал `400`, когда запрос нарушал ограничения. Теперь клиенту нужно получать предсказуемое JSON-описание ошибки. `@RestControllerAdvice` регистрирует общий обработчик, а `@ExceptionHandler(ТипИсключения.class)` выбирает метод для конкретной ошибки. Код контроллера при этом не должен обрастать `try/catch`.

Рабочий пример на отдельном случае:

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

record ErrorView(String code) {}

@RestController
class SquareController {
    @GetMapping("/squares/{number}")
    int square(@PathVariable("number") int number) {
        if (number < 0) throw new IllegalArgumentException();
        return number * number;
    }
}

@RestControllerAdvice
class NumberErrorHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorView> negativeNumber(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorView("negative_number"));
    }
}
```

- `@RestController` и `@GetMapping` принимают запрос, `@PathVariable` извлекает число. При отрицательном числе контроллер выбрасывает исключение.
- `@RestControllerAdvice` делает второй класс обработчиком ошибок контроллеров. `@ExceptionHandler` связывает его метод именно с `IllegalArgumentException`.
- `ResponseEntity.badRequest().body(...)` возвращает `400` и JSON с полем `code`; `try/catch` в контроллере не нужен.

## Что сделать

В пакете `learning.task062` готовы:

- `PlayerApplication.java` — запуск Spring Boot;
- `PlayerRequest.java` — входящий `record` с уже заданными ограничениями на `name` и `level`;
- `PlayerPreview.java` — модель успешного ответа;
- `PlayerPreviewController.java` — готовый `POST /players/preview` с `@Valid`;
- `ApiError.java` — готовый `record ApiError(String code, List<String> fields)`.

Реализуй только `ApiErrorHandler.java`:

- Пометь класс `@RestControllerAdvice`.
- Для `MethodArgumentNotValidException` верни `400` и `ApiError("invalid_request", fields)`. Поля возьми из `exception.getBindingResult().getFieldErrors()`: у каждого `FieldError` есть `getField()`. Убери повторы и отсортируй имена по возрастанию.
- Для `HttpMessageNotReadableException` верни `400` и `ApiError("malformed_json", List.of())`.
- Не меняй готовые классы и не добавляй ручную проверку в контроллер. Текст исключения клиенту не отправляй.

## Примеры

- `POST /players/preview`, `{"name":"Мира","level":10}` → `200`, `{"name":"Мира","level":10}`.
- `POST /players/preview`, `{"name":" ","level":0}` → `400`, `{"code":"invalid_request","fields":["level","name"]}`.
- `POST /players/preview` с телом `{broken` → `400`, `{"code":"malformed_json","fields":[]}`.

## Ограничения и готовность

- Java 21, Spring Boot 3.5; существующей зависимости валидации достаточно.
- Тесты проверяют успех, одиночные и несколько нарушений, сортировку/уникальность полей, неверный JSON, отсутствие тела и маршруты.

## Файлы

- Решение: `src/main/java/learning/task062/ApiErrorHandler.java`.
- Готовые файлы: `src/main/java/learning/task062/PlayerApplication.java`, `PlayerRequest.java`, `PlayerPreview.java`, `PlayerPreviewController.java`, `ApiError.java`.
- Тесты: `src/test/java/learning/task062/ApiErrorHandlerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
