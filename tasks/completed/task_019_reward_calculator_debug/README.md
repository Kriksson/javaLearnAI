# Задача №19: отладка расчёта награды

- Сложность: высокая
- Закрепление: числа, условия, статические методы, исключения и unit-тесты
- Новое понятие: поиск причины ошибки по падающему тесту

## Контекст

В игре есть готовый расчётчик опыта за квест. Он компилируется, но содержит
несколько ошибок. Твоя задача — прочитать условие, запустить тесты, найти
конкретную причину каждой ошибки и исправить реализацию.

Не меняй публичные сигнатуры `RewardCalculator` и не переписывай тесты.

## `RewardCalculator`

```java
public final class RewardCalculator
public static int calculate(int baseExperience,
                            int completedObjectives,
                            int totalObjectives,
                            boolean premium)
```

Правила расчёта:

1. `baseExperience` не может быть отрицательным.
2. `totalObjectives` должно быть положительным.
3. `completedObjectives` должно быть от `0` до `totalObjectives` включительно.
4. Базовая награда пропорциональна выполненной части квеста:
   `baseExperience * completedObjectives / totalObjectives`.
   Используется целочисленное деление, дробная часть отбрасывается.
5. Если квест выполнен полностью и `premium == true`, к результату добавляется
   бонус 20%. Дробная часть бонуса отбрасывается.
6. Во всех некорректных входных данных выбрасывай `IllegalArgumentException`.

## Примеры

```java
RewardCalculator.calculate(100, 1, 4, false); // 25
RewardCalculator.calculate(100, 3, 3, true);  // 120
RewardCalculator.calculate(101, 3, 3, true);  // 121
RewardCalculator.calculate(100, 1, 4, true);  // 25
```

## Как работать

1. Запусти `./mvnw test`.
2. Прочитай имя упавшего теста и ожидаемое значение.
3. Сопоставь его с формулой в условии.
4. Исправь только причину ошибки.
5. Повтори запуск до успешного результата.

## Ограничения и критерии готовности

- не меняй публичные сигнатуры и тесты;
- класс остаётся `final`, а его конструктор — `private`;
- не добавляй `double` и `float`: расчёт целочисленный;
- все тесты проходят.

## Файлы

- Исправляемый код: `src/main/java/learning/task019/RewardCalculator.java`;
- Тесты: `src/test/java/learning/task019/RewardCalculatorTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
