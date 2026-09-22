# Задание 33 — Очередь обращений с приоритетом

**Сложность:** следующий уровень Java Collections

## Закрепляем

- `enum` с данными;
- `Comparator` по нескольким полям;
- `Optional` для отсутствующего элемента;
- `Set` для уникальных идентификаторов.

## Новое понятие: `PriorityQueue`

`PriorityQueue<T>` хранит элементы так, чтобы через `poll()` получить самый
приоритетный. В отличие от `Deque`, порядок определяется `Comparator`, а не
только временем добавления.

Важно: обход `PriorityQueue` через `for-each` не гарантирует отсортированный
порядок. Чтобы получить упорядоченный снимок, сделай копию очереди и извлекай
элементы из неё через `poll()`.

## Задача

Реализуй очередь обращений в игровую поддержку.

### `TicketPriority`

Создай enum:

```java
URGENT(3), NORMAL(2), LOW(1)
```

Добавь поле `weight` и геттер.

### `SupportTicket`

Создай record:

```java
public record SupportTicket(String id, TicketPriority priority, long createdOrder) { }
```

Правила:

- `id` не `null`, после `trim()` не пустой и сохраняется без крайних пробелов;
- `priority` не `null`;
- `createdOrder >= 0`;
- нарушение — `IllegalArgumentException`.

### `SupportQueue`

Реализуй:

```java
public boolean submit(SupportTicket ticket)
public Optional<SupportTicket> next()
public int size()
public List<SupportTicket> waiting()
```

Правила порядка:

1. больший `priority.weight()` раньше;
2. при равном приоритете меньший `createdOrder` раньше;
3. при равных значениях `id` по алфавиту.

Остальные правила:

- `submit(null)` — `IllegalArgumentException`;
- повторный `id` не добавляется и возвращает `false`;
- `next()` извлекает следующий билет и возвращает `Optional.empty()`, если
  очередь пуста;
- после `next()` его `id` можно добавить снова;
- `waiting()` возвращает неизменяемый список в порядке, в котором билеты были
  бы выданы через `next()`.

## Пример

```text
LOW, createdOrder 0
URGENT, createdOrder 5
URGENT, createdOrder 2

next() → URGENT с createdOrder 2
```

## Файлы

- `src/main/java/learning/task033/TicketPriority.java`;
- `src/main/java/learning/task033/SupportTicket.java`;
- `src/main/java/learning/task033/SupportQueue.java`;
- `src/test/java/learning/task033/SupportQueueTest.java`.

Запуск: `bash ./mvnw test`.
