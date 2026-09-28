# Задача №59 — Удаление игрока через Spring Boot

**Сложность:** средняя.

**Закрепляем:** `@PathVariable`, `ResponseEntity`, `Map`, проверку ID, HTTP-статусы и сохранение состояния при ошибке.

**Впервые:** `@DeleteMapping` — обработка HTTP `DELETE`; `204 No Content` — операция успешна, но в ответе нет тела.

## Материал для изучения

`DELETE /players/1` обращается к конкретному ресурсу так же, как знакомый `GET /players/1`, но удаляет его. Успешное удаление не требует JSON-ответа: `ResponseEntity.noContent().build()` вернёт статус `204` и пустое тело. Если ресурс не найден, в этой задаче возвращается `404`. Повторный `DELETE` не изменит состояние ещё раз, хотя статус будет уже `404`.

Отдельный пример для другого ресурса:

```java
@RestController
class LabelController {
    private final Map<Integer, String> labels = new HashMap<>(Map.of(1, "важно"));

    @DeleteMapping("/labels/{id}")
    ResponseEntity<Void> delete(@PathVariable("id") int id) {
        if (labels.remove(id) == null) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}
```

`DELETE /labels/1` вернёт `204`; тот же запрос ещё раз вернёт `404`. В задаче с игроками есть дополнительное бизнес-правило: нельзя удалять игрока с активным матчем.

## Что сделать

В пакете `learning.task059` уже готовы `PlayerApplication.java`, `PlayerView.java` и чтение игрока через `GET /players/{id}` в `PlayerController.java`. Реализуй только метод:

```java
public ResponseEntity<Void> delete(@PathVariable("id") int id)
```

- В контроллере хранятся игроки `1` (`Мира`), `2` (`Тор`), `3` (`Ари`). Игрок `2` находится в наборе `activeMatches`. Не меняй начальные данные и готовый `GET`.
- `id <= 0` → `400` без тела.
- Игрок с таким ID отсутствует → `404` без тела.
- Игрок найден, но его ID находится в `activeMatches` → `409 Conflict` без тела; состояние не меняется.
- Иначе удали игрока из `players` и верни `204 No Content` без тела. После этого `GET` и повторный `DELETE` по этому ID возвращают `404`.
- Проверяй условия в указанном порядке: неверный ID, отсутствие игрока, активный матч, удаление.
- Не удаляй записи из `activeMatches`; в рамках задания это неизменяемые сведения об активных матчах.

## Примеры

- `DELETE /players/1` → `204`, затем `GET /players/1` → `404`.
- `DELETE /players/2` → `409`, затем `GET /players/2` → `200`: игрок остался.
- `DELETE /players/9` → `404`; `DELETE /players/0` → `400`.

## Ограничения и готовность

- Java 21 и существующий Spring Boot 3.5; новых зависимостей нет.
- Тесты проверяют успешное и повторное удаление, сохранение других игроков, конфликт, границы ID, пустое тело и неверный маршрут/метод.

## Файлы

- Решение: `src/main/java/learning/task059/PlayerController.java`.
- Готовые файлы: `src/main/java/learning/task059/PlayerApplication.java`, `PlayerView.java`.
- Тесты: `src/test/java/learning/task059/PlayerControllerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
