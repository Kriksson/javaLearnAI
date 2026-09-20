# Задача №11: уникальные игроки

- Сложность: высокая
- Закрепление: классы, инкапсуляция, конструкторы, исключения и коллекции
- Новые понятия: методы `equals`, `hashCode`, `toString` и коллекция `Set`

## Контекст

Один и тот же игрок может быть загружен из разных источников и оказаться в
памяти как два разных объекта. Его игровая личность определяется `id`, а не
ссылкой на объект и не отображаемым именем.

Реализуй `Player` и `PlayerRegistry`, чтобы реестр хранил только уникальных
игроков.

## `Player`

```java
public Player(String id, String displayName)
public String getId()
public String getDisplayName()
public boolean equals(Object other)
public int hashCode()
public String toString()
```

Требования:

- поля `id` и `displayName` — `private final`;
- перед сохранением оба значения очищаются через `trim()`;
- пустой `id` или `displayName` вызывает `IllegalArgumentException`;
- два игрока равны, когда у них одинаковый `id`, даже если имена различаются;
- игрок не равен `null` и объекту другого типа;
- `hashCode()` должен быть согласован с `equals`: равные игроки дают одинаковый
  хеш-код;
- `toString()` возвращает строку `id (displayName)`.

## `PlayerRegistry`

```java
public PlayerRegistry()
public boolean register(Player player)
public int size()
public boolean contains(Player player)
```

Требования:

- используй `Set<Player>` и подходящую реализацию из стандартной библиотеки;
- `register` добавляет игрока и возвращает `true`, только если такого `id` ещё
  не было; при повторной регистрации возвращает `false`;
- `contains` определяет игрока по тем же правилам равенства;
- `register(null)` и `contains(null)` выбрасывают `IllegalArgumentException`.

## Примеры

```java
Player first = new Player(" p-42 ", " Kirill ");
Player loadedAgain = new Player("p-42", "Kriksson");

first.equals(loadedAgain); // true
first.toString();          // "p-42 (Kirill)"

PlayerRegistry registry = new PlayerRegistry();
registry.register(first);       // true
registry.register(loadedAgain); // false
registry.size();                // 1
```

## Ограничения и критерии готовности

- `Player` и `PlayerRegistry` находятся в отдельных файлах;
- не сравнивай строки через `==`;
- не ищи дубликаты циклом: за уникальность отвечает `Set`;
- `equals`, `hashCode` и `toString` пометь `@Override`;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task011/Player.java`,
  `src/main/java/learning/task011/PlayerRegistry.java`;
- Тесты: `src/test/java/learning/task011/PlayerRegistryTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
