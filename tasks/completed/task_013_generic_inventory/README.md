# Задача №13: универсальный инвентарь

- Сложность: высокая
- Закрепление: классы, инкапсуляция, `List`, `ArrayList`, поиск и исключения
- Новое понятие: дженерики — параметр типа `T`

## Контекст

Инвентарь может хранить строки с названиями предметов, объекты персонажей или
любые другие сущности. Не нужно создавать отдельный класс для каждого типа:
один обобщённый класс `Inventory<T>` сохранит тип элементов и поможет компилятору
не допустить ошибку.

## `Inventory<T>`

```java
public class Inventory<T>
public Inventory()
public void add(T item)
public T get(int index)
public boolean remove(T item)
public boolean contains(T item)
public int size()
```

Требования:

- класс объявлен с параметром типа `T`;
- элементы хранятся в `List<T>` с помощью `ArrayList`;
- `add` не принимает `null` и выбрасывает `IllegalArgumentException`;
- `get` возвращает элемент по индексу; неверный индекс должен приводить к
  стандартному `IndexOutOfBoundsException` списка;
- `remove` удаляет первое совпадение и возвращает `true`; если элемента нет,
  возвращает `false`;
- `contains` возвращает, хранится ли такой элемент; `null` передавать нельзя —
  выбрасывай `IllegalArgumentException`;
- размер меняется после добавления и успешного удаления.

## Примеры

```java
Inventory<String> potions = new Inventory<>();
potions.add("health potion");
potions.add("mana potion");
potions.get(0);                 // "health potion"
potions.remove("mana potion"); // true

Inventory<Player> players = new Inventory<>();
players.add(new Player("p-42", "Kirill"));
```

Во втором примере `Player` показан только как идея: для решения задачи создавать
этот класс не нужно. Важно, что один `Inventory<T>` работает с обоими типами.

## Ограничения и критерии готовности

- не используй «сырой» тип `List` без `<T>`;
- не приводи элементы вручную через `(String)`, `(Object)` и подобные касты;
- поле со списком должно быть `private`;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task013/Inventory.java`;
- Тесты: `src/test/java/learning/task013/InventoryTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
