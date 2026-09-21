# Задача №14: отсортированный рейтинг

- Сложность: высокая
- Закрепление: классы, `List`, `ArrayList`, `Collections` и исключения
- Новые понятия: интерфейс `Comparable<T>` и естественный порядок объектов

## Контекст

В рейтинге игроки должны располагаться по очкам: чем больше очков, тем выше
позиция. При равных очках порядок должен быть стабильным и понятным — по
`playerId` в алфавитном порядке.

Реализуй `ScoreEntry` и `Leaderboard`.

## `ScoreEntry`

```java
public class ScoreEntry implements Comparable<ScoreEntry>
public ScoreEntry(String playerId, int score)
public String getPlayerId()
public int getScore()
public int compareTo(ScoreEntry other)
```

Требования:

- поля `playerId` и `score` — `private final`;
- `playerId` очищается через `trim()` и не может быть пустым;
- очки не могут быть отрицательными; неверные аргументы вызывают
  `IllegalArgumentException`;
- класс реализует `Comparable<ScoreEntry>`;
- `compareTo` задаёт такой порядок:
  1. больше очков — раньше в рейтинге;
  2. при одинаковых очках меньший по алфавиту `playerId` — раньше;
  3. одинаковые `playerId` и очки считаются равными для сортировки.

Для сравнения чисел используй `Integer.compare`, а не вычитание.

## `Leaderboard`

```java
public Leaderboard()
public void add(ScoreEntry entry)
public int size()
public List<ScoreEntry> top(int count)
```

Требования:

- хранит записи в `List<ScoreEntry>`;
- `add(null)` выбрасывает `IllegalArgumentException`;
- `top(count)` возвращает новый список из не более чем `count` лучших записей;
- для сортировки используй `Collections.sort(...)`, чтобы она вызвала
  `ScoreEntry.compareTo`;
- порядок добавления во внутреннем списке не должен меняться после вызова
  `top`;
- отрицательный `count` вызывает `IllegalArgumentException`; при `count == 0`
  возвращается пустой список.

## Примеры

```java
Leaderboard board = new Leaderboard();
board.add(new ScoreEntry("mira", 120));
board.add(new ScoreEntry("alex", 150));
board.add(new ScoreEntry("boris", 120));

board.top(2); // alex (150), boris (120)
```

## Ограничения и критерии готовности

- `ScoreEntry` и `Leaderboard` находятся в отдельных файлах;
- `compareTo` пометь `@Override`;
- не изменяй внутренний список рейтинга при чтении `top`;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task014/ScoreEntry.java`,
  `src/main/java/learning/task014/Leaderboard.java`;
- Тесты: `src/test/java/learning/task014/LeaderboardTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
