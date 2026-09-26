# Задача №57 — Обновление профиля игрока через Spring Boot

**Сложность:** средняя.

**Закрепляем:** `@GetMapping`, `@PathVariable`, `@RequestBody`, `ResponseEntity`, `Map`, проверки входных данных и JSON.

**Впервые:** `@PutMapping` — обработка HTTP `PUT`, которым клиент задаёт новое состояние существующего ресурса. Одинаковый `PUT`, отправленный повторно, должен оставить тот же результат (идемпотентность).

## Материал для изучения

`POST` в прошлой задаче вычислял результат запроса. Теперь нужно изменить профиль, который хранится на сервере. Spring направляет `PUT` в метод с `@PutMapping`; ID приходит из пути через `@PathVariable`, а новые поля — из JSON через `@RequestBody`. Для уже известного `GET` останется отдельный метод.

Независимый пример для другого ресурса:

```java
@RestController
class FlagController {
    private final Map<Integer, Boolean> flags = new HashMap<>();

    @PutMapping("/flags/{id}")
    ResponseEntity<Void> put(@PathVariable int id, @RequestBody FlagRequest request) {
        flags.put(id, request.enabled());
        return ResponseEntity.ok().build();
    }
}

record FlagRequest(boolean enabled) {}
```

Запрос `PUT /flags/7` с `{"enabled":true}` сохранит значение по ключу `7`. Повторение запроса оставит то же значение. В этой задаче **новые** ID создавать нельзя: перед сохранением проверь наличие игрока в `Map`.

## Что сделать

В пакете `learning.task057` готовы `PlayerApplication.java`, `PlayerUpdateRequest.java` (`String name, Integer level`) и `PlayerView.java` (`int id, String name, int level`). Реализуй только `PlayerController.java`:

- В контроллере уже создана `Map<Integer, PlayerView>`: игрок `1` — `Мира`, уровень `10`; игрок `2` — `Тор`, уровень `20`. Не меняй начальные данные.
- `GET /players/{id}`: `id <= 0` → `400` без тела; отсутствующий игрок → `404` без тела; существующий → `200` и его `PlayerView` как JSON.
- `PUT /players/{id}` с JSON `{"name":"...","level":...}`: сначала проверь ID и наличие игрока с теми же статусами. Затем проверь тело: `name` не `null`, после `trim()` не пустое и не длиннее 20 символов; `level` не `null` и лежит от `1` до `100` включительно. Нарушение → `400` без тела, состояние не меняется.
- При успехе сохрани `new PlayerView(id, очищенноеИмя, level)` под тем же ID и верни `200` с обновлённым `PlayerView`. Повторный такой же `PUT` не должен менять результат.
- Не разбирай и не собирай JSON вручную. Используй данные из готовых `record` и стандартную сериализацию Spring.

## Примеры

- `GET /players/1` → `200`, `{"id":1,"name":"Мира","level":10}`.
- `PUT /players/1`, `{"name":"  Лиса  ","level":11}` → `200`, `{"id":1,"name":"Лиса","level":11}`; последующий `GET /players/1` вернёт то же.
- `PUT /players/9`, `{"name":"Лиса","level":11}` → `404`; `PUT /players/1`, `{"name":" ","level":11}` → `400`.

## Ограничения и готовность

- Java 21, уже подключённый Spring Boot 3.5; новых зависимостей нет.
- Тесты покрывают чтение, обновление, повторный `PUT`, границы имени и уровня, ошибки, неизменность состояния после ошибки, неверный метод и JSON.

## Файлы

- Решение: `src/main/java/learning/task057/PlayerController.java`.
- Готовые модели и запуск: `src/main/java/learning/task057/PlayerApplication.java`, `PlayerUpdateRequest.java`, `PlayerView.java`.
- Тесты: `src/test/java/learning/task057/PlayerControllerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
