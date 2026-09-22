# Задание 28 — Статистика добычи

**Сложность:** следующий уровень Java Collections

## Закрепляем

- `record` и проверку входных данных;
- `Map<String, Integer>` для агрегации значений;
- сортировку данных по нескольким правилам;
- неизменяемые снимки коллекций.

## Новое понятие: `Map.merge`

`merge(key, value, remappingFunction)` добавляет значение для нового ключа или
объединяет его с существующим. Для суммирования количества подходит идея:
«если предмет уже есть, прибавь новую порцию».

## Задача

Реализуй статистику добычи игрока.

### `LootDrop`

Создай record:

```java
public record LootDrop(String itemName, int quantity) { }
```

Требования:

- имя не `null`, после `trim()` не пустое;
- `quantity > 0`;
- некорректные данные приводят к `IllegalArgumentException`;
- сохранённое имя не содержит пробелов по краям.

### `LootStatistics`

Реализуй:

```java
public void register(LootDrop drop)
public int totalFor(String itemName)
public Map<String, Integer> totals()
public List<String> mostCommon(int limit)
```

Правила:

- `register` добавляет количество из `drop` к уже накопленному количеству
  предмета; `null` — `IllegalArgumentException`;
- `totalFor` принимает непустое имя, очищает пробелы по краям и возвращает `0`,
  если предмета ещё не было;
- `totals()` возвращает неизменяемый снимок текущей статистики;
- `mostCommon(limit)` возвращает до `limit` названий: сперва большее количество,
  при равенстве — имя по алфавиту;
- `limit < 0` — `IllegalArgumentException`, `limit == 0` — неизменяемый пустой список.

## Пример

После регистрации:

```text
gold: 5
potion: 3
gold: 2
arrow: 7
```

`totalFor(" gold ")` вернёт `7`, а `mostCommon(3)` — `arrow`, `gold`, `potion`.

## Файлы

- `src/main/java/learning/task028/LootDrop.java`;
- `src/main/java/learning/task028/LootStatistics.java`;
- `src/test/java/learning/task028/LootStatisticsTest.java`.

Запуск: `bash ./mvnw test`.
