# Задача №5: класс игрового персонажа

- Сложность: очень высокая
- Закрепление: строки, методы, условия и проверка данных
- Новые навыки: отдельные классы, поля, конструктор, инкапсуляция, состояние объекта и исключения

## Условие

Создай отдельный публичный класс `GameCharacter`. Объект хранит состояние персонажа и управляет им через методы.

## Поля

Класс должен хранить:

- `nickname` — ник без пробелов по краям;
- `characterClass` — класс персонажа без пробелов по краям;
- `level` — текущий уровень.

Все поля должны быть `private`. Ник и класс после создания не изменяются, уровень может увеличиваться.

## Конструктор

```java
public GameCharacter(String nickname, String characterClass, int level)
```

Конструктор должен:

1. убрать пробелы по краям ника и класса;
2. выбросить `IllegalArgumentException`, если ник или класс пуст;
3. выбросить `IllegalArgumentException`, если уровень меньше 1 или больше 100;
4. сохранить проверенные значения в полях.

Аргументы `nickname` и `characterClass` не равны `null`.

## Методы

```java
public String getNickname()
public String getCharacterClass()
public int getLevel()
public void levelUp()
public String getRank()
public String getProfile()
```

- Геттеры возвращают значения полей.
- `levelUp()` увеличивает уровень на 1. Уровень 100 остаётся равным 100.
- `getRank()` возвращает:
  - уровни 1–9: `NOVICE`;
  - уровни 10–24: `VETERAN`;
  - уровни 25–49: `ELITE`;
  - уровни 50–100: `LEGEND`.
- `getProfile()` возвращает строку `[РАНГ] Ник — Класс, уровень N`.

## Примеры

```text
new GameCharacter("  Kirill  ", " Mage ", 9)
getNickname() → "Kirill"
getRank() → "NOVICE"
levelUp()
getRank() → "VETERAN"
getProfile() → "[VETERAN] Kirill — Mage, уровень 10"
```

```text
new GameCharacter("Tank", "Warrior", 100)
levelUp()
getLevel() → 100
```

## Критерии готовности

- `GameCharacter` находится в отдельном файле.
- Поля закрыты модификатором `private`.
- Конструктор очищает и проверяет данные.
- Повышение уровня соблюдает максимум 100.
- Ранги правильно меняются на всех границах.
- Профиль соответствует точному формату.
- Все тесты проходят.

## Файлы

- Архив решения: `GameCharacter.java`
- Архив тестов: `GameCharacterTest.java`

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
