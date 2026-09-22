# Задание 30 — Каталог игроков и `Optional`

**Сложность:** следующий уровень проектирования API

## Закрепляем

- `record` и валидацию данных;
- поиск сущностей в `Map`;
- сортировку списка и неизменяемый результат.

## Новое понятие: `Optional`

`Optional<T>` явно показывает, что результат может отсутствовать. Это лучше,
чем возвращать `null`: вызывающий код видит возможность отсутствия в сигнатуре.

Полезные методы:

```java
optional.isPresent()
optional.orElse(значениеПоУмолчанию)
```

В этой задаче `findById` должен вернуть `Optional.empty()`, если игрока нет.

## Задача

Реализуй каталог игроков.

### `PlayerProfile`

Создай record:

```java
public record PlayerProfile(String id, String nickname, int level) { }
```

Правила:

- `id` и `nickname` не `null`, после `trim()` не пустые;
- оба значения сохраняются без пробелов по краям;
- `level` от `1` до `100` включительно;
- некорректные данные — `IllegalArgumentException`.

### `PlayerDirectory`

Реализуй методы:

```java
public void register(PlayerProfile profile)
public Optional<PlayerProfile> findById(String id)
public int size()
public List<PlayerProfile> withMinimumLevel(int minimumLevel)
```

Правила:

- `register` добавляет профиль по его `id`; повторная регистрация с тем же `id`
  заменяет старый профиль;
- `findById` принимает непустой `id`, очищает пробелы по краям;
- `withMinimumLevel` принимает значение от `1` до `100`, возвращает игроков с
  уровнем не ниже заданного; порядок: больший уровень выше, при равенстве —
  никнейм по алфавиту;
- список из `withMinimumLevel` должен быть неизменяемым.

## Пример

```text
register(" p-1 ", " Mira ", 20)
findById("p-1") → Optional с профилем Mira
findById("p-9") → Optional.empty()
```

## Файлы

- `src/main/java/learning/task030/PlayerProfile.java`;
- `src/main/java/learning/task030/PlayerDirectory.java`;
- `src/test/java/learning/task030/PlayerDirectoryTest.java`.

Запуск: `bash ./mvnw test`.
