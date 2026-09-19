# Задача №6: отряд персонажей

- Сложность: высокая
- Закрепление: отдельные классы, конструкторы, инкапсуляция, методы, условия и исключения
- Новые навыки: композиция объектов, `List`, `ArrayList`, цикл `for-each`, поиск и агрегирование данных

## Контекст

Класс `GameCharacter` уже содержит твоё решение предыдущей задачи. Изменять его не требуется.

Создай отдельный класс `Party`, который управляет отрядом игровых персонажей.

## Состояние `Party`

Класс должен хранить в приватных полях:

- название отряда;
- максимальное количество участников;
- коллекцию участников типа `List<GameCharacter>`.

Для создания коллекции используй `ArrayList`.

## Конструктор

```java
public Party(String name, int maxSize)
```

Конструктор должен убрать пробелы по краям названия и выбросить `IllegalArgumentException`, если название пусто или `maxSize` меньше 1.

## Методы

```java
public String getName()
public int getSize()
public boolean addMember(GameCharacter character)
public GameCharacter findMember(String nickname)
public GameCharacter getStrongestMember()
public double getAverageLevel()
```

### `addMember`

- Добавляет персонажа и возвращает `true`.
- Возвращает `false`, если отряд заполнен.
- Возвращает `false`, если участник с таким ником уже есть.
- При сравнении ников игнорируй регистр.
- Аргумент `character` не равен `null`.

### `findMember`

- Ищет участника по нику без учёта регистра и пробелов по краям аргумента.
- Возвращает найденный объект или `null`.

### `getStrongestMember`

- Возвращает участника с максимальным уровнем.
- При одинаковом уровне возвращает добавленного раньше.
- Для пустого отряда возвращает `null`.

### `getAverageLevel`

- Возвращает среднее арифметическое уровней как `double`.
- Для пустого отряда возвращает `0.0`.

## Пример

```text
Party party = new Party("  Night Watch  ", 2)
party.addMember(new GameCharacter("Kirill", "Mage", 20)) → true
party.addMember(new GameCharacter("Tank", "Warrior", 35)) → true
party.addMember(new GameCharacter("Archer", "Ranger", 15)) → false

party.getName() → "Night Watch"
party.getSize() → 2
party.findMember(" kirill ") → персонаж Kirill
party.getStrongestMember() → персонаж Tank
party.getAverageLevel() → 27.5
```

## Ограничения

- `Party` и `GameCharacter` должны оставаться отдельными классами.
- Не используй массив вместо коллекции.
- Не возвращай внутренний список наружу.
- Не меняй публичные сигнатуры.

## Критерии готовности

- Конструктор проверяет параметры.
- Вместимость и уникальность ников соблюдаются.
- Поиск игнорирует регистр.
- Сильнейший участник и средний уровень вычисляются правильно.
- Особые случаи пустого отряда обработаны.
- Все тесты проходят.

## Файлы

- Архив решения: `Party.java`
- Базовый класс: `GameCharacter.java`
- Архив тестов: `PartyTest.java`

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
