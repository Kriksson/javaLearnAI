# Задача №61 — Проверка заявки через Bean Validation

**Сложность:** средняя.

**Закрепляем:** `@PostMapping`, `@RequestBody`, `record`, JSON-ответ и проверку граничных случаев.

**Впервые:** ограничения Jakarta Bean Validation на полях запроса и `@Valid` для запуска их проверки в Spring MVC.

## Материал для изучения

Раньше ты писал `if` для проверки каждого поля JSON. Для типовых правил можно объявить ограничения рядом с полями модели. Нужная зависимость `spring-boot-starter-validation` уже добавлена в `pom.xml`. Аннотации сами по себе не проверяют HTTP-запрос: параметр контроллера нужно отметить `@Valid`. Если правило нарушено, Spring возвращает `400` до выполнения тела метода.

Рабочий пример на отдельной теме:

```java
record BookRequest(
        @NotBlank @Size(max = 30) String title,
        @NotNull @Min(1) @Max(500) Integer pages
) {}

@RestController
class BookController {
    @PostMapping("/books/check")
    String check(@Valid @RequestBody BookRequest request) {
        return request.title() + ": " + request.pages();
    }
}
```

- `@NotBlank` требует непустую строку с непробельным символом; `@Size(max = 30)` ограничивает её длину. Эти две аннотации относятся к `title`.
- `@NotNull` требует число; `@Min(1)` и `@Max(500)` задают включительные границы `pages`. Тип `Integer` позволяет отличить пропущенное поле от числа.
- `@PostMapping` выбирает маршрут, `@RequestBody` превращает JSON в `BookRequest`, а `@Valid` проверяет ограничения до вызова `check`.
- Последняя строка работает только для принятого запроса. Некорректный JSON Spring тоже отклоняет, но это проверка разбора JSON, а не Bean Validation.

## Что сделать

В пакете `learning.task061`:

- `MatchApplication.java` — готовый запуск Spring Boot, не меняй.
- `MatchRequest.java` — добавь ограничения к компонентам `record MatchRequest(String playerName, Integer level, Integer opponentId)`: `playerName` не `null`, не пустой и не состоящий только из пробелов, длина не более `20`; `level` не `null`, от `1` до `100` включительно; `opponentId` не `null` и не меньше `1`. Используй аннотации из примера, а не ручные `if` для этих правил.
- `MatchPreview.java` — готовый ответ `record MatchPreview(String playerName, int opponentId, boolean ranked)`.
- `MatchController.java` — у метода `POST /matches/preview` добавь `@Valid` к параметру `@RequestBody MatchRequest request`. Для допустимого запроса верни `200` и `MatchPreview` с исходным `playerName`, исходным `opponentId` и `ranked = true`, если `level >= 50`, иначе `false`. Пробелы по краям имени не удаляй: принятое значение передаётся в ответ без изменения.
- При нарушении ограничений ожидается `400`. Неверный синтаксис JSON и неподходящие типы Spring отклоняет отдельно. Не добавляй ручной разбор JSON и не меняй готовые модели.

## Примеры

- `POST /matches/preview`, `{"playerName":"Мира","level":49,"opponentId":2}` → `200`, `{"playerName":"Мира","opponentId":2,"ranked":false}`.
- `POST /matches/preview`, `{"playerName":" Тор ","level":50,"opponentId":1}` → `200`, `{"playerName":" Тор ","opponentId":1,"ranked":true}`.
- `POST /matches/preview`, `{"playerName":"   ","level":50,"opponentId":1}` → `400`; пропущенный `level` → `400`.

## Ограничения и готовность

- Java 21, Spring Boot 3.5 и только добавленная зависимость валидации.
- Тесты проверяют нормальные ответы, границы каждого поля, отсутствие и неверные значения, ошибки JSON и HTTP-маршрут.

## Файлы

- Решение: `src/main/java/learning/task061/MatchRequest.java`, `MatchController.java`.
- Готовые файлы: `src/main/java/learning/task061/MatchApplication.java`, `MatchPreview.java`.
- Тесты: `src/test/java/learning/task061/MatchControllerTest.java`.
- Зависимость уже добавлена: `pom.xml`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
