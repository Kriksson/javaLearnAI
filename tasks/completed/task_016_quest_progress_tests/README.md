# Задача №16: прогресс квеста и unit-тесты

- Сложность: высокая
- Закрепление: классы, состояние объекта, условия, исключения и граничные значения
- Новые понятия: unit-тесты JUnit 5, структура Arrange–Act–Assert

## Контекст

Игрок выполняет квест по шагам. Класс `QuestProgress` хранит состояние одного
квеста, а тесты должны доказать, что нормальные случаи, границы и ошибки
обрабатываются правильно.

В этой задаче ты реализуешь класс и самостоятельно пишешь часть JUnit-тестов.
Готовые контрактные тесты проверят итоговое поведение, а твои тесты нужны для
тренировки разработки через проверяемые сценарии.

## `QuestProgress`

```java
public QuestProgress(String title, int targetSteps)
public String getTitle()
public int getTargetSteps()
public int getCompletedSteps()
public int getRemainingSteps()
public boolean isCompleted()
public void addProgress(int steps)
```

Требования:

- `title` очищается через `trim()` и не может быть пустым;
- `targetSteps` должен быть положительным; иначе `IllegalArgumentException`;
- начальный выполненный прогресс равен `0`;
- `addProgress` принимает только положительное число, иначе
  `IllegalArgumentException`;
- прогресс увеличивается, но не может стать больше цели;
- `getRemainingSteps()` возвращает число оставшихся шагов;
- `isCompleted()` возвращает `true`, только когда цель достигнута.

## Твои тесты

В `QuestProgressTest.java` напиши минимум четыре теста с `@Test`:

1. создание квеста и начальное состояние;
2. обычное добавление прогресса;
3. достижение цели и ограничение прогресса сверху;
4. хотя бы один ошибочный сценарий через `assertThrows`.

Используй структуру Arrange–Act–Assert:

```java
// Arrange — подготовить объект
// Act — вызвать проверяемый метод
// Assert — проверить результат
```

Имена тестов должны описывать ожидаемое поведение, например
`addProgressCapsValueAtTarget`.

## Примеры

```java
QuestProgress quest = new QuestProgress("  Find relic  ", 5);
quest.getCompletedSteps(); // 0
quest.getRemainingSteps(); // 5

quest.addProgress(3);
quest.getRemainingSteps(); // 2
quest.isCompleted();       // false

quest.addProgress(10);
quest.getCompletedSteps(); // 5
quest.isCompleted();       // true
```

## Ограничения и критерии готовности

- `QuestProgress` и `QuestProgressTest` находятся в отдельных файлах;
- поля `QuestProgress` должны быть `private`;
- не используй `System.out.println` для проверки поведения;
- добавь минимум четыре собственных теста в `QuestProgressTest`;
- все тесты, включая контрактные, проходят.

## Файлы

- Решение: `src/main/java/learning/task016/QuestProgress.java`;
- Твои тесты: `src/test/java/learning/task016/QuestProgressTest.java`;
- Контрактные тесты: `src/test/java/learning/task016/QuestProgressContractTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
