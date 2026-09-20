# Задача №12: игровые сессии

- Сложность: высокая
- Закрепление: классы, конструкторы, инкапсуляция, условия и исключения
- Новые понятия: перечисления `enum`, статические поля, методы и константы

## Контекст

Игровая платформа создаёт сессии разных режимов. У каждого режима свой лимит
игроков, а платформа должна знать общее число созданных сессий независимо от
того, в каком объекте это число запрашивается.

Реализуй перечисление `GameMode` и класс `GameSession`.

## `GameMode`

```java
public enum GameMode
SOLO(1), DUO(2), SQUAD(4)
public int getMaxPlayers()
```

Требования:

- создай три значения: `SOLO`, `DUO`, `SQUAD`;
- у каждого хранится максимальное число игроков: соответственно `1`, `2`, `4`;
- поле лимита — `private final`;
- `getMaxPlayers()` возвращает лимит своего режима.

## `GameSession`

```java
public static final int MAX_TITLE_LENGTH = 20
public GameSession(String title, GameMode mode)
public int getId()
public String getTitle()
public GameMode getMode()
public boolean canJoin(int currentPlayers)
public static int getCreatedCount()
```

Требования:

- идентификатор сессии начинается с `1` и увеличивается на `1` при каждом
  создании новой сессии;
- счётчик созданных сессий общий для всех объектов и доступен через статический
  метод `getCreatedCount()`;
- `MAX_TITLE_LENGTH` — публичная статическая неизменяемая константа со значением
  `20`;
- перед сохранением название очищается через `trim()`;
- пустое название или название длиннее `MAX_TITLE_LENGTH` вызывает
  `IllegalArgumentException`;
- `mode == null` вызывает `IllegalArgumentException`;
- `canJoin(currentPlayers)` возвращает `true`, если игроков меньше лимита
  режима; отрицательное число игроков вызывает `IllegalArgumentException`.

## Примеры

```java
GameSession solo = new GameSession("  Training ", GameMode.SOLO);
solo.getId();       // 1 — если это первая сессия
solo.getTitle();    // "Training"
solo.canJoin(0);    // true
solo.canJoin(1);    // false

GameSession squad = new GameSession("Raid", GameMode.SQUAD);
squad.canJoin(3);                   // true
GameSession.getCreatedCount();       // 2 — если созданы только эти сессии
```

## Ограничения и критерии готовности

- `GameMode` и `GameSession` находятся в отдельных файлах;
- поля экземпляра должны быть `private`; неизменяемые значения делай `final`;
- не передавай счётчик в конструктор: это общее состояние класса;
- для проверки лимита используй `mode.getMaxPlayers()`;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task012/GameMode.java`,
  `src/main/java/learning/task012/GameSession.java`;
- Тесты: `src/test/java/learning/task012/GameSessionTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
