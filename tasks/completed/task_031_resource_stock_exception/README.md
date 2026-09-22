# Задание 31 — Склад ресурсов и собственное исключение

**Сложность:** следующий уровень обработки ошибок

## Закрепляем

- агрегацию данных в `Map`;
- неизменяемый снимок состояния;
- валидацию аргументов.

## Новое понятие: собственное checked-исключение

`IllegalArgumentException` подходит для неверного аргумента метода. Но иногда
аргумент корректный, а операция невозможна из-за текущего состояния системы.

Например, запросить 5 кристаллов корректно, но на складе может быть только 2.
Для этого создай `InsufficientStockException extends Exception`. Так как это
checked-исключение, метод `reserve` обязан указать его через `throws`, а
вызывающий код — обработать или объявить его.

## Задача

Реализуй склад игровых ресурсов.

### `InsufficientStockException`

Создай класс с конструктором:

```java
public InsufficientStockException(String resourceName, int requested, int available)
```

Он должен наследоваться от `Exception` и передавать в `super(...)` понятное
сообщение, в котором встречаются имя ресурса, запрошенное и доступное количество.

### `ResourceStock`

Реализуй методы:

```java
public void add(String resourceName, int quantity)
public void reserve(String resourceName, int quantity) throws InsufficientStockException
public int available(String resourceName)
public Map<String, Integer> snapshot()
```

Правила:

- имя ресурса не `null`, после `trim()` не пустое; количество строго больше 0;
  нарушение — `IllegalArgumentException`;
- `add` суммирует количество одноимённых ресурсов;
- `reserve` уменьшает остаток. Если ресурса меньше, чем запрошено, выбрасывает
  `InsufficientStockException` и **не меняет** склад;
- при успешном резервировании остаток `0` нужно удалить из `Map`;
- `available` для отсутствующего ресурса возвращает `0`;
- `snapshot()` возвращает неизменяемую копию состояния.

## Пример

```text
add(" crystal ", 5)
reserve("crystal", 3)
available("crystal") = 2
reserve("crystal", 3) → InsufficientStockException
available("crystal") всё ещё = 2
```

## Файлы

- `src/main/java/learning/task031/InsufficientStockException.java`;
- `src/main/java/learning/task031/ResourceStock.java`;
- `src/test/java/learning/task031/ResourceStockTest.java`.

Запуск: `bash ./mvnw test`.
