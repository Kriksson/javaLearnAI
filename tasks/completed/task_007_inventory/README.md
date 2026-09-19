# Задача №7: инвентарь игрока

- Сложность: высокая
- Закрепление: отдельные классы, коллекции, циклы, поиск, проверка данных и агрегирование
- Новые навыки: `enum`, `Map`, `HashMap`, `entrySet`, ключи и значения

## Контекст

`ItemType` и `Item` уже готовы. Изменять их не требуется.

Реализуй отдельный класс `Inventory`, который хранит предметы и их количество.

## Состояние `Inventory`

Инвентарь должен содержать приватное поле:

```java
Map<Item, Integer> items
```

Для создания коллекции используй `HashMap`.

## Методы

```java
public void addItem(Item item, int amount)
public boolean removeItem(Item item, int amount)
public int getQuantity(Item item)
public int getDistinctItemCount()
public int getTotalItemCount()
public int getTotalValue()
public int getTotalValueByType(ItemType type)
public Item findByName(String name)
```

### `addItem`

- Добавляет указанное количество предметов.
- Если такой ключ уже есть, увеличивает его количество.
- При `amount <= 0` выбрасывает `IllegalArgumentException`.
- Аргумент `item` не равен `null`.

### `removeItem`

- При `amount <= 0` выбрасывает `IllegalArgumentException`.
- Возвращает `false`, если предмета нет или количества недостаточно.
- Иначе уменьшает количество и возвращает `true`.
- Если количество стало равно нулю, полностью удаляет ключ из `Map`.

### Подсчёты

- `getQuantity` возвращает количество конкретного предмета или `0`.
- `getDistinctItemCount` возвращает число разных ключей.
- `getTotalItemCount` возвращает сумму количества всех предметов.
- `getTotalValue` возвращает сумму `цена × количество` по всему инвентарю.
- `getTotalValueByType` считает стоимость только предметов заданного типа.

### `findByName`

- Ищет предмет по имени без учёта регистра и пробелов по краям аргумента.
- Возвращает найденный объект или `null`.

## Пример

```text
Item potion = new Item("Health Potion", ItemType.POTION, 50)
Item sword = new Item("Iron Sword", ItemType.WEAPON, 120)

inventory.addItem(potion, 3)
inventory.addItem(sword, 1)

inventory.getQuantity(potion) → 3
inventory.getDistinctItemCount() → 2
inventory.getTotalItemCount() → 4
inventory.getTotalValue() → 270
inventory.getTotalValueByType(ItemType.POTION) → 150
inventory.findByName(" health potion ") → potion
```

## Ограничения

- Все классы и `enum` находятся в отдельных файлах.
- Не используй `List` или массив вместо `Map`.
- Поле коллекции должно быть `private`.
- Не возвращай внутреннюю `Map` наружу.
- Не меняй публичные сигнатуры.

## Критерии готовности

- Одинаковые предметы объединяются в одной записи.
- Удаление правильно обрабатывает недостаток и нулевой остаток.
- Все виды подсчётов работают для пустого и заполненного инвентаря.
- Поиск игнорирует регистр.
- Все тесты проходят.

## Файлы

- Архив решения: `Inventory.java`
- Готовые типы: `Item.java`, `ItemType.java`
- Архив тестов: `InventoryTest.java`

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
