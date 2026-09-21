# Задача №15: мастерская улучшений

- Сложность: высокая
- Закрепление: классы, инкапсуляция, коллекции и полиморфные вызовы
- Новое понятие: интерфейс `interface`

## Контекст

Мастерская умеет улучшать оружие и броню. У предметов разная характеристика и
разный предел уровня, но для мастерской важен общий контракт: узнать название,
уровень и попытаться улучшить предмет.

Реализуй интерфейс `Upgradeable`, классы `Weapon`, `Armor` и `Workshop`.

## `Upgradeable`

```java
public interface Upgradeable
String getName()
int getLevel()
boolean upgrade()
```

- `upgrade()` повышает уровень на один и возвращает `true`;
- если предмет уже достиг максимального уровня, его состояние не меняется и
  метод возвращает `false`.

## `Weapon`

```java
public Weapon(String name, int basePower)
public int getPower()
```

- реализует `Upgradeable`;
- название очищается через `trim()` и не может быть пустым;
- базовая сила должна быть положительной; иначе `IllegalArgumentException`;
- начальный уровень — `0`, максимальный — `3`;
- `getPower()` возвращает `basePower + level * 5`.

## `Armor`

```java
public Armor(String name, int baseDefense)
public int getDefense()
```

- реализует `Upgradeable`;
- название очищается через `trim()` и не может быть пустым;
- базовая защита не может быть отрицательной; иначе `IllegalArgumentException`;
- начальный уровень — `0`, максимальный — `2`;
- `getDefense()` возвращает `baseDefense + level * 3`.

## `Workshop`

```java
public Workshop()
public void add(Upgradeable item)
public int size()
public int upgradeAll()
```

- хранит предметы в `List<Upgradeable>`;
- `add(null)` выбрасывает `IllegalArgumentException`;
- `upgradeAll()` вызывает `upgrade()` у каждого предмета и возвращает число
  предметов, которые действительно улучшились;
- не используй `instanceof` и приведения типов: мастерская работает только через
  методы интерфейса.

## Примеры

```java
Upgradeable sword = new Weapon("  Iron sword ", 10);
sword.upgrade(); // true
sword.getLevel(); // 1

Weapon weapon = new Weapon("Iron sword", 10);
weapon.upgrade();
weapon.getPower(); // 15

Workshop workshop = new Workshop();
workshop.add(weapon);
workshop.add(new Armor("Leather", 4));
workshop.upgradeAll(); // 2
```

## Ограничения и критерии готовности

- `Upgradeable`, `Weapon`, `Armor` и `Workshop` находятся в отдельных файлах;
- поля классов должны быть `private`;
- методы интерфейса в классах пометь `@Override`;
- не добавляй `instanceof` в `Workshop`;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task015/Upgradeable.java`, `Weapon.java`,
  `Armor.java`, `Workshop.java`;
- Тесты: `src/test/java/learning/task015/WorkshopTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
