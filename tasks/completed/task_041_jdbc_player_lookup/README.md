# Задача №41 — Поиск игрока через JDBC

**Сложность:** переход от SQL к Java и базе данных

**Закрепляем:** `SELECT`, `WHERE`, `Optional`, `record`, проверку аргументов и checked-исключения.

**Новое:** JDBC, `PreparedStatement`, `ResultSet` и параметр `?` в SQL.

Реализуй поиск игрока по никнейму в уже созданной таблице `players`. Тесты поднимут базу H2 в режиме совместимости с PostgreSQL; для приложения с PostgreSQL используется тот же JDBC API.

## Материал для изучения

JDBC — стандартный API Java для работы с реляционной базой. `Connection` представляет соединение. Через `PreparedStatement` выполняют SQL с параметрами: знак `?` означает место для значения, а `setString(1, value)` передаёт строку в первый параметр. Это позволяет безопасно искать даже строки с апострофом.

```java
PreparedStatement statement = connection.prepareStatement(
        "SELECT stock FROM products WHERE name = ?");
statement.setString(1, productName);
ResultSet rows = statement.executeQuery();
```

`ResultSet` сначала расположен перед первой строкой. `rows.next()` переходит к следующей строке и возвращает `false`, если строк больше нет. Значения читают по имени столбца, например `rows.getInt("stock")`. Закрывай `PreparedStatement` и `ResultSet` через `try-with-resources`; переданное в репозиторий соединение остаётся у вызывающего кода.

## Что сделать

В пакете `learning.task041` находятся два отдельных файла:

- `Player.java` — готовая модель `record Player(int id, String nickname, int level)`;
- `PlayerRepository.java` — класс, который нужно реализовать.

Контракт `PlayerRepository`:

```java
public PlayerRepository(Connection connection)
public Optional<Player> findByNickname(String nickname) throws SQLException
```

- Конструктор сохраняет переданное соединение. Если оно равно `null`, выбрасывает `IllegalArgumentException`.
- Метод ищет точное совпадение никнейма через параметризованный `SELECT` по таблице `players` (`id`, `nickname`, `level`). Регистр и пробелы не изменяй.
- Если игрок найден, верни `Optional` с `Player`; если нет — `Optional.empty()`.
- Если `nickname` равен `null`, пуст или состоит только из пробелов, выбрасывай `IllegalArgumentException`.
- Ошибки базы данных передавай вызывающему коду как `SQLException`. Не закрывай переданный `Connection`.

## Примеры

При данных `Kira` (id 1, уровень 42) и `O'Neil` (id 2, уровень 17):

- `findByNickname("Kira")` → `Optional` с `Player(1, "Kira", 42)`;
- `findByNickname("O'Neil")` → `Optional` с `Player(2, "O'Neil", 17)`;
- `findByNickname("Unknown")` → `Optional.empty()`;
- `findByNickname("   ")` → `IllegalArgumentException`.

## Ограничения и готовность

- Используй `PreparedStatement` с `?` и закрывай его вместе с `ResultSet`.
- Не изменяй модель `Player` и таблицу, создаваемую тестом.
- Тесты проверяют существующего и отсутствующего игрока, апостроф в никнейме, точное совпадение, неверные аргументы, ошибку закрытого соединения и то, что поиск не закрывает соединение.
- Новые зависимости не нужны.

## Файлы

- Модель: `src/main/java/learning/task041/Player.java`
- Решение: `src/main/java/learning/task041/PlayerRepository.java`
- Автотесты: `src/test/java/learning/task041/PlayerRepositoryTest.java`

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
