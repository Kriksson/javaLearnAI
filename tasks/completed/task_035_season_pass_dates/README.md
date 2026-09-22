# Задание 35 — Сезонные пропуска и `java.time`

**Сложность:** современный Java — дата и время

## Что изучаем впервые

### `LocalDate`

`LocalDate` представляет календарную дату без времени и часового пояса:

```java
LocalDate.of(2026, 9, 22)
```

Для сравнения дат используй `isBefore`, `isAfter` и `isEqual`.

### `ChronoUnit.DAYS.between`

Метод считает число дней между двумя датами. Если обе границы должны входить в период, к результату прибавляют `1`.

## Задача

Реализуй календарь сезонных пропусков игроков.

### `SeasonPass`

Создай record:

```java
public record SeasonPass(String playerId, LocalDate startsOn, LocalDate endsOn) { }
```

Правила:

- `playerId` не `null`, после `trim()` не пустой и сохраняется без пробелов по краям;
- даты не `null`;
- `startsOn` не позже `endsOn`;
- нарушение — `IllegalArgumentException`.

### `SeasonPassCalendar`

Реализуй:

```java
public List<String> activePlayerIds(List<SeasonPass> passes, LocalDate date)
public long daysRemaining(SeasonPass pass, LocalDate date)
```

Правила:

- `passes` и `date` не `null`; `null`-элементы списка игнорируются;
- пропуск активен в день `date`, если дата лежит в диапазоне от `startsOn` до `endsOn` включительно;
- `activePlayerIds` возвращает неизменяемый список идентификаторов по алфавиту;
- `daysRemaining` возвращает число активных календарных дней, включая `date` и `endsOn`; если пропуск уже закончился, возвращает `0`;
- `pass == null` для `daysRemaining` — `IllegalArgumentException`.

Метод `activePlayerIds` реализуй через Stream API, без `for` и `while`.

## Пример

```text
pass: 2026-09-20 .. 2026-09-22
date: 2026-09-22
activePlayerIds → содержит игрока
daysRemaining → 1
```

## Файлы

- `src/main/java/learning/task035/SeasonPass.java`;
- `src/main/java/learning/task035/SeasonPassCalendar.java`;
- `src/test/java/learning/task035/SeasonPassCalendarTest.java`.

Запуск: `bash ./mvnw test`.
