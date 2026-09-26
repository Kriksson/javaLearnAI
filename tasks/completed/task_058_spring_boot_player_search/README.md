# Задача №58 — Поиск игроков через параметры запроса

**Сложность:** средняя.

**Закрепляем:** `@GetMapping`, `ResponseEntity`, `record`, фильтрацию коллекции и типизированный JSON-ответ.

**Впервые:** `@RequestParam` — получение параметров после `?` в URL; возврат списка объектов как JSON-массива.

## Материал для изучения

Путь обозначает ресурс, а параметры запроса уточняют выборку: `/players?minLevel=20&name=тор`. Spring преобразует строковые значения параметров в объявленные типы, например `Integer`. Для необязательного параметра укажи `required = false`: когда его нет, в метод придёт `null`. Имя параметра укажи явно, так как в сборке проекта имена Java-параметров недоступны Spring через рефлексию.

Независимый пример:

```java
@RestController
class ItemController {
    @GetMapping("/items")
    List<String> items(@RequestParam(value = "prefix", required = false) String prefix) {
        List<String> items = List.of("меч", "щит");
        if (prefix == null) return items;
        return items.stream().filter(item -> item.startsWith(prefix)).toList();
    }
}
```

`GET /items` вернёт все строки, а `GET /items?prefix=ме` — только `"меч"`. Список Spring самостоятельно превратит в JSON-массив. В этой задаче дополнительно нужны проверки параметров и статус `400` при недопустимых значениях.

## Что сделать

В `learning.task058` реализуй метод `search` в `PlayerSearchController.java` для `GET /players`:

```java
public ResponseEntity<List<PlayerView>> search(
        @RequestParam(value = "minLevel", required = false) Integer minLevel,
        @RequestParam(value = "name", required = false) String name)
```

- В контроллере уже есть неизменяемый список из пяти игроков. Начальные данные не меняй.
- Без параметров верни `200` и всех игроков по возрастанию `id`.
- Если передан `minLevel`, он должен быть от `1` до `100` включительно; оставь игроков с `level >= minLevel`. Отсутствие параметра не ограничивает уровень.
- Если передан `name`, сначала примени `trim()`. Результат должен быть непустым и не длиннее 20 символов. Оставь игроков, чьё имя содержит эту подстроку без учёта регистра (`Locale.ROOT`). Отсутствие параметра не ограничивает имя.
- Оба фильтра применяются одновременно. Результат всегда отсортирован по `id` по возрастанию. Если совпадений нет, верни `200` и пустой JSON-массив `[]`.
- Неверный диапазон `minLevel` или неверный `name` → `400` без тела. Нечисловой `minLevel` Spring отклонит самостоятельно.
- Не собирай JSON вручную, не добавляй зависимости и не меняй готовые модели.

## Примеры

- `GET /players` → `200`, массив всех пяти игроков с ID `1, 2, 3, 4, 5`.
- `GET /players?minLevel=20&name=тор` → `200`, игроки с ID `2, 5`.
- `GET /players?name=несуществующий` → `200`, `[]`; `GET /players?minLevel=0` → `400` без тела.

## Ограничения и готовность

- Java 21 и существующий Spring Boot 3.5.
- Тесты проверяют отсутствие фильтров, каждый фильтр и их сочетание, границы, регистр, пробелы, пустой результат, некорректные параметры и маршрут.

## Файлы

- Решение: `src/main/java/learning/task058/PlayerSearchController.java`.
- Готовые файлы: `src/main/java/learning/task058/PlayerApplication.java`, `PlayerView.java`.
- Тесты: `src/test/java/learning/task058/PlayerSearchControllerTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
