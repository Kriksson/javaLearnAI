# Задача №60 — Начисление очков через сервис Spring Boot

**Сложность:** средняя.

**Закрепляем:** `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestBody`, `Map`, проверки данных и HTTP-статусы.

**Впервые:** `@Service` и внедрение зависимости через конструктор.

## Материал для изучения

Раньше контроллер сам хранил игроков и менял их состояние. Теперь обязанности разделены: контроллер принимает HTTP-запрос и выбирает статус ответа, а сервис хранит игроков и выполняет начисление. Spring создаёт объект класса с `@Service` и передаёт его в конструктор контроллера; вручную писать `new PlayerService()` в контроллере не нужно.

Рабочий пример на другой теме:

```java
@Service
class TaxService {
    int withTax(int price) {
        return price + price / 5;
    }
}

@RestController
class PriceController {
    private final TaxService taxService;

    PriceController(TaxService taxService) {
        this.taxService = taxService;
    }

    @GetMapping("/prices/{price}")
    int price(@PathVariable("price") int price) {
        return taxService.withTax(price);
    }
}
```

- `@Service` регистрирует `TaxService` в Spring. Метод `withTax` содержит вычисление, не связанное с HTTP.
- Поле `taxService` хранит зависимость контроллера. В параметр конструктора Spring подставляет созданный им сервис; присваивание сохраняет его в поле.
- `@GetMapping` выбирает маршрут; `@PathVariable` берёт число из URL; последняя строка делегирует расчёт сервису. Контроллер не вычисляет налог сам.

## Что сделать

В пакете `learning.task060`:

- `PlayerApplication.java` — готовая конфигурация Spring Boot, не меняй.
- `PlayerView.java` — готовая модель `record PlayerView(int id, String name, int score)`.
- `PointAwardRequest.java` — готовая модель входящего JSON `record PointAwardRequest(Integer points)`.
- `PlayerService.java` — добавь `@Service`. Начальная `Map` уже содержит игрока `1` (`Мира`, 10 очков) и игрока `2` (`Тор`, 50 очков). Реализуй `find(int id)`: верни игрока или `null`, если его нет. Реализуй `award(int id, int points)`: увеличь счёт существующего игрока, сохрани новый `PlayerView` в `Map` и верни его; если игрока нет, верни `null` и не создавай его. Весь доступ к `Map` остаётся внутри сервиса.
- `PlayerController.java` — конструктор для внедрения `PlayerService` уже готов. Реализуй `GET /players/{id}` и `POST /players/{id}/points`.

Правила контроллера:

- Для обоих маршрутов `id <= 0` → `400` без тела; отсутствующий игрок → `404` без тела.
- `GET` для существующего игрока → `200` и текущий `PlayerView`.
- `POST` принимает JSON `{"points":5}`. После проверки ID и наличия игрока проверь `points`: не `null`, от `1` до `20` включительно. Ошибка → `400` без тела, счёт не меняется.
- Успешный `POST` вызывает сервис, возвращает `200` и обновлённый `PlayerView`. Повторный запрос начисляет очки ещё раз.
- Не заводи в контроллере вторую `Map` и не создавай сервис через `new`. Не разбирай JSON вручную.

## Примеры

- `GET /players/1` → `200`, `{"id":1,"name":"Мира","score":10}`.
- `POST /players/1/points`, `{"points":5}` → `200`, `{"id":1,"name":"Мира","score":15}`; повторение → счёт `20`.
- `POST /players/9/points`, `{"points":5}` → `404`; `POST /players/1/points`, `{"points":0}` → `400` без изменения счёта.

## Ограничения и готовность

- Java 21 и существующий Spring Boot 3.5; новых зависимостей нет.
- Тесты проверяют регистрацию сервиса, его методы, чтение, повторное начисление, границы, ошибки, отсутствие изменения при неудаче и маршруты.

## Файлы

- Решение: `src/main/java/learning/task060/PlayerService.java`, `PlayerController.java`.
- Готовые файлы: `src/main/java/learning/task060/PlayerApplication.java`, `PlayerView.java`, `PointAwardRequest.java`.
- Тесты: `src/test/java/learning/task060/PlayerControllerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
