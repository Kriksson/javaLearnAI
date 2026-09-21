# Задача №18: валидатор никнеймов и параметризованные тесты

- Сложность: высокая
- Закрепление: строки, циклы, статические методы, условия и JUnit 5
- Новые понятия: `@ParameterizedTest`, `@ValueSource`, `@NullSource`

## Контекст

Перед созданием профиля игра проверяет никнейм. Один набор правил применяется
ко многим значениям, поэтому вместо множества одинаковых тестов используй
параметризованные тесты JUnit 5.

Реализуй `NicknameValidator` и напиши собственные тесты.

## `NicknameValidator`

```java
public final class NicknameValidator
public static boolean isValid(String nickname)
```

Правила:

- `null` — недопустимое значение, метод возвращает `false`;
- перед проверкой строка очищается через `trim()`;
- длина очищенного никнейма от `3` до `12` символов включительно;
- первый символ — английская буква (`A-Z` или `a-z`);
- остальные символы могут быть английскими буквами, цифрами или `_`;
- любой другой символ делает никнейм недопустимым.

Примеры:

```java
NicknameValidator.isValid("  Hero_42  "); // true
NicknameValidator.isValid("42Hero");      // false
NicknameValidator.isValid("ab");          // false
NicknameValidator.isValid("Hero-name");   // false
```

Не используй регулярные выражения: проверь символы циклом.

## Твои параметризованные тесты

В `NicknameValidatorTest.java` напиши минимум два параметризованных теста.

Допустимые значения можно передавать так:

```java
@ParameterizedTest
@ValueSource(strings = {"Hero", "mage_7", "A12"})
void acceptsValidNicknames(String nickname) {
    // assertTrue(...)
}
```

Для недопустимых строк используй второй `@ParameterizedTest` с `@ValueSource`.
Отдельно проверь `null` с `@NullSource` в этом же тесте.

## Ограничения и критерии готовности

- класс находится в `NicknameValidator.java`, а тесты — в
  `NicknameValidatorTest.java`;
- класс сделай `final`, конструктор — `private`;
- метод `isValid` должен быть `public static`;
- в своих тестах используй минимум два `@ParameterizedTest` и `@NullSource`;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task018/NicknameValidator.java`;
- Твои тесты: `src/test/java/learning/task018/NicknameValidatorTest.java`;
- Контрактные тесты: `src/test/java/learning/task018/NicknameValidatorContractTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
