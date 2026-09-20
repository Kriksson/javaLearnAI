# Задача №10: абстрактный боевой отряд

- Сложность: высокая
- Закрепление: наследование, инкапсуляция, `List`, обход коллекции и полиморфные вызовы
- Новые понятия: абстрактный класс и абстрактный метод

## Контекст

В предыдущей задаче у каждого боевого класса была общая основа `GameUnit`.
Теперь сделай её абстрактной: отдельный объект просто «боевой юнит» создавать
нельзя, но с каждым конкретным юнитом можно работать через тип `CombatUnit`.

Реализуй `Knight`, `Archer` и `BattleSquad`. В отряде должны храниться
`CombatUnit`, поэтому расчёт атаки обязан работать с разными потомками без
проверок `instanceof`.

## `CombatUnit`

```java
public abstract class CombatUnit
public CombatUnit(String name, int health)
public String getName()
public int getHealth()
public boolean isAlive()
public void takeDamage(int damage)
public abstract int getAttackPower()
public abstract String getUnitType()
public String getDescription()
```

Требования:

- `CombatUnit` — абстрактный класс;
- имя после `trim()` не должно быть пустым, а начальное здоровье должно быть
  положительным; иначе выбрасывай `IllegalArgumentException`;
- сохрани очищенное имя и начальное здоровье;
- `takeDamage` принимает только неотрицательный урон и уменьшает здоровье до
  нуля, не ниже;
- живой юнит имеет здоровье больше нуля;
- `getDescription()` возвращает строку
  `имя [ТИП], HP: здоровье`, где `ТИП` получен вызовом `getUnitType()`.

## `Knight`

```java
public Knight(String name, int armor)
public int getArmor()
```

- наследуется от `CombatUnit` и вызывает `super(name, 110)`;
- броня допустима от `0` до `15` включительно, иначе `IllegalArgumentException`;
- `getAttackPower()` возвращает `17`;
- `getUnitType()` возвращает `KNIGHT`.

## `Archer`

```java
public Archer(String name, int arrows)
public int getArrows()
public boolean shoot(CombatUnit target)
```

- наследуется от `CombatUnit` и вызывает `super(name, 70)`;
- число стрел допустимо от `0` до `10` включительно, иначе `IllegalArgumentException`;
- `getAttackPower()` возвращает `12`, если есть хотя бы одна стрела, иначе `3`;
- `getUnitType()` возвращает `ARCHER`;
- `shoot` при наличии стрелы уменьшает её число на 1, наносит цели `12` урона и
  возвращает `true`; без стрел ничего не меняет и возвращает `false`.

Считай, что `target` в `shoot` не равен `null`.

## `BattleSquad`

```java
public BattleSquad()
public void add(CombatUnit unit)
public int size()
public int getAliveAttackPower()
```

- хранит юнитов в `List<CombatUnit>`;
- `add` не принимает `null` и выбрасывает `IllegalArgumentException`;
- `getAliveAttackPower()` возвращает сумму `getAttackPower()` только живых юнитов;
- не используй `instanceof` и приведения типов: нужный метод выбирается
  полиморфно.

## Примеры

```java
CombatUnit knight = new Knight("  Bronn  ", 6);
knight.getDescription(); // "Bronn [KNIGHT], HP: 110"

Archer archer = new Archer("Lia", 1);
archer.shoot(knight);    // true
knight.getHealth();      // 98
archer.getAttackPower(); // 3 — стрел больше нет

BattleSquad squad = new BattleSquad();
squad.add(knight);
squad.add(archer);
squad.getAliveAttackPower(); // 20
```

## Ограничения и критерии готовности

- `CombatUnit`, `Knight`, `Archer` и `BattleSquad` находятся в отдельных файлах;
- поля должны быть `private`;
- абстрактные методы реализуй с `@Override` в дочерних классах;
- все тесты проходят.

## Файлы

- Решение: `src/main/java/learning/task010/CombatUnit.java`, `Knight.java`,
  `Archer.java`, `BattleSquad.java`;
- Тесты: `src/test/java/learning/task010/BattleSquadTest.java`.

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
