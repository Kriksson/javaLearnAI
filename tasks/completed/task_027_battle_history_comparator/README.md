# Задание 27 — История боёв и `Comparator`

**Сложность:** следующий уровень Java Collections

## Закрепляем

- `List` и защиту внутренней коллекции от внешних изменений;
- сортировку копии списка;
- валидацию данных в конструкторе.

## Новые понятия

- `record` — компактный неизменяемый класс для хранения данных;
- `Comparator` — отдельное правило порядка, когда естественного порядка
  `Comparable` недостаточно или он не нужен классу всегда.

## Задача

Реализуй историю результатов боёв.

### `MatchResult`

Создай `record`:

```java
public record MatchResult(String playerName, int score, int durationSeconds) { }
```

В компактном конструкторе проверь:

- `playerName` не `null` и не состоит только из пробелов;
- `score >= 0`;
- `durationSeconds > 0`.

При нарушении выбрасывай `IllegalArgumentException`.

### `BattleHistory`

Реализуй методы:

```java
public void add(MatchResult result)
public int size()
public List<MatchResult> top(int limit)
public List<MatchResult> all()
```

`top(limit)` возвращает не более `limit` результатов в порядке:

1. больший `score` выше;
2. при равном счёте меньший `durationSeconds` выше;
3. при равных счёте и времени имя идёт по алфавиту.

Особые случаи:

- `limit < 0` — `IllegalArgumentException`;
- `limit == 0` — пустой список;
- `limit` больше числа записей — все записи;
- вызов `top` не меняет порядок добавления и возвращает неизменяемый список.

`all()` возвращает все результаты в порядке добавления. Этот список тоже должен
быть неизменяемым.

## Пример

Для результатов:

```text
Mira, 120, 80
Alex, 120, 75
Zed, 100, 40
```

`top(2)` вернёт `Alex`, затем `Mira`.

## Файлы

- `src/main/java/learning/task027/MatchResult.java`;
- `src/main/java/learning/task027/BattleHistory.java`;
- `src/test/java/learning/task027/BattleHistoryTest.java`.

Запуск: `bash ./mvnw test`.
