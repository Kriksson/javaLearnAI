# Задача №17: журнал квестов

- Сложность: высокая, мини-проект
- Закрепление: классы, `enum`, `Map`, инкапсуляция, исключения и unit-тесты
- Новое понятие: модель состояний и допустимые переходы между ними

## Контекст

Игровой журнал хранит квесты по уникальному идентификатору. Каждый квест
проходит путь `AVAILABLE → ACTIVE → COMPLETED`. Нельзя завершить не начатый
квест, повторно начать активный или изменить уже завершённый.

Реализуй `QuestStatus`, `Quest` и `QuestJournal`. Затем дополни свои тесты.

## `QuestStatus`

```java
public enum QuestStatus {
    AVAILABLE,
    ACTIVE,
    COMPLETED
}
```

## `Quest`

```java
public Quest(String id, String title)
public String getId()
public String getTitle()
public QuestStatus getStatus()
public void start()
public void complete()
```

Требования:

- `id` и `title` очищаются через `trim()` и не могут быть пустыми;
- новый квест получает статус `AVAILABLE`;
- `start()` переводит квест из `AVAILABLE` в `ACTIVE`;
- `complete()` переводит квест из `ACTIVE` в `COMPLETED`;
- любой недопустимый переход выбрасывает `IllegalStateException` и не меняет
  статус.

## `QuestJournal`

```java
public QuestJournal()
public void add(Quest quest)
public int size()
public Quest getById(String id)
public void startQuest(String id)
public void completeQuest(String id)
public int getCompletedCount()
```

Требования:

- хранит квесты в `Map<String, Quest>`;
- `add(null)` выбрасывает `IllegalArgumentException`;
- идентификатор уникален: повторное добавление квеста с тем же `id` выбрасывает
  `IllegalArgumentException`;
- `getById`, `startQuest` и `completeQuest` очищают аргумент через `trim()`;
- пустой или неизвестный `id` в этих методах вызывает `IllegalArgumentException`;
- `getCompletedCount()` возвращает число квестов в статусе `COMPLETED`.

## Пример

```java
QuestJournal journal = new QuestJournal();
journal.add(new Quest(" relic ", " Find the relic "));

journal.startQuest("relic");
journal.completeQuest(" relic ");

journal.getById("relic").getStatus(); // COMPLETED
journal.getCompletedCount();            // 1
```

## Твои тесты

В `QuestJournalTest.java` напиши минимум три собственных теста:

1. успешный путь `AVAILABLE → ACTIVE → COMPLETED`;
2. недопустимый переход с `assertThrows`;
3. добавление и поиск квеста в журнале.

Контрактные тесты проверят остальную спецификацию.

## Ограничения и критерии готовности

- `QuestStatus`, `Quest` и `QuestJournal` находятся в отдельных файлах;
- поля доменных классов должны быть `private`;
- не используй `if` по строковому имени статуса: сравнивай значения `enum`;
- добавь минимум три собственных JUnit-теста;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task017/QuestStatus.java`, `Quest.java`,
  `QuestJournal.java`;
- Твои тесты: `src/test/java/learning/task017/QuestJournalTest.java`;
- Контрактные тесты: `src/test/java/learning/task017/QuestJournalContractTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
