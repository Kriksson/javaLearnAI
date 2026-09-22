# Задание 32 — Очередь матчмейкинга

**Сложность:** мини-проект на Java Collections

## Закрепляем

- `record` и валидацию;
- `Set` для уникальности игроков;
- неизменяемые списки результата.

## Новое понятие: `Deque`

`Deque` — двусторонняя очередь. Для обычной очереди используй:

- `addLast(...)` — поставить игрока в конец;
- `removeFirst()` — взять игрока, который ждёт дольше всех.

Подходящая реализация — `ArrayDeque`.

## Задача

Реализуй очередь игроков для матчей.

### `QueueTicket`

Создай record:

```java
public record QueueTicket(String playerId, int rating) { }
```

Правила:

- `playerId` не `null`, после `trim()` не пустой и сохраняется без пробелов по краям;
- `rating` от `0` до `5000` включительно;
- некорректные данные — `IllegalArgumentException`.

### `MatchmakingQueue`

Реализуй методы:

```java
public boolean join(QueueTicket ticket)
public boolean cancel(String playerId)
public List<QueueTicket> startMatch(int teamSize)
public int waitingCount()
public List<QueueTicket> waitingPlayers()
```

Правила:

- `join` добавляет игрока в конец очереди и возвращает `true`; если игрок с тем
  же `playerId` уже ждёт, ничего не меняет и возвращает `false`;
- `cancel` удаляет игрока по непустому `playerId`; возвращает `true`, если игрок
  был в очереди, иначе `false`;
- `startMatch` требует `teamSize > 0`. Если игроков меньше, возвращает
  неизменяемый пустой список и не меняет очередь. Иначе забирает ровно
  `teamSize` первых игроков и возвращает неизменяемый список;
- `waitingPlayers` возвращает неизменяемый снимок в порядке ожидания.

## Пример

```text
join(Mira) → true
join(Alex) → true
join(Mira) → false
startMatch(2) → [Mira, Alex]
waitingCount() → 0
```

## Файлы

- `src/main/java/learning/task032/QueueTicket.java`;
- `src/main/java/learning/task032/MatchmakingQueue.java`;
- `src/test/java/learning/task032/MatchmakingQueueTest.java`.

Запуск: `bash ./mvnw test`.
