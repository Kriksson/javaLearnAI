# Задание 34 — Аналитика урона через Stream API

**Сложность:** новый этап — современный Java

## Что изучаем впервые

### Лямбда-выражение

Лямбда — короткая запись функции. Например:

```java
event -> event.damage() > 0
```

Она принимает `event` и возвращает `true` или `false`.

### Stream API

`stream()` создаёт последовательность обработки данных. Типичный конвейер:

```text
источник → filter → groupingBy / sorting → результат
```

Для этой задачи пригодятся:

- `stream()`;
- `filter(...)`;
- `collect(Collectors.groupingBy(..., Collectors.summingInt(...)))`;
- `sorted(...)`, `limit(...)`, `map(...)`, `toList()`.

## Задача

Реализуй аналитику боевого лога без обычных циклов `for` и `while` в методах
`totalDamageByPlayer` и `topPlayers`.

### `DamageEvent`

Создай record:

```java
public record DamageEvent(String playerId, int damage) { }
```

Правила:

- `playerId` не `null`, после `trim()` не пустой и хранится без крайних пробелов;
- `damage >= 0`;
- нарушение — `IllegalArgumentException`.

### `DamageAnalytics`

Реализуй:

```java
public Map<String, Integer> totalDamageByPlayer(List<DamageEvent> events)
public List<String> topPlayers(List<DamageEvent> events, int limit)
```

Правила:

- `events == null` — `IllegalArgumentException`;
- элементы `null` в списке игнорируются;
- события с `damage == 0` не учитываются;
- `totalDamageByPlayer` возвращает неизменяемую `Map` с суммой положительного
  урона по каждому игроку;
- `topPlayers` принимает `limit >= 0`, возвращает игроков с наибольшим суммарным
  уроном; при равной сумме — `playerId` по алфавиту;
- `limit == 0` возвращает неизменяемый пустой список;
- оба метода не меняют входной список.

## Пример

```text
Mira: 10, 5
Alex: 15
Zed: 0

totalDamageByPlayer → {Mira=15, Alex=15}
topPlayers(..., 2) → [Alex, Mira]
```

## Файлы

- `src/main/java/learning/task034/DamageEvent.java`;
- `src/main/java/learning/task034/DamageAnalytics.java`;
- `src/test/java/learning/task034/DamageAnalyticsTest.java`.

Запуск: `bash ./mvnw test`.
