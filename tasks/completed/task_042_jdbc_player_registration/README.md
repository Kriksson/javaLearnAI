# Задача №42 — Регистрация игрока через JDBC

**Сложность:** JDBC, базовый уровень

**Закрепляем:** `PreparedStatement`, валидацию аргументов, `record`, `SQLException`, `try-with-resources`.

**Новое:** запись через `executeUpdate()` и получение ID через `getGeneratedKeys()`.

Реализуй сохранение нового игрока в таблице `players`. База сама создаёт `id`; метод должен вернуть модель с этим ID.

## Материал для изучения

Для `INSERT`, `UPDATE` и `DELETE` JDBC использует `executeUpdate()`: результат — количество затронутых строк. Значения передают через параметры `?`, как в задаче №41.

Когда ID создаёт база, при подготовке запроса можно указать `Statement.RETURN_GENERATED_KEYS`. После `executeUpdate()` вызови `getGeneratedKeys()` и прочитай первую строку `ResultSet` — в её первом столбце находится новый ID.

```java
PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO products (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
statement.setString(1, productName);
statement.executeUpdate();
ResultSet keys = statement.getGeneratedKeys();
```

Этот фрагмент показывает порядок вызовов; в своём методе закрывай и `PreparedStatement`, и `ResultSet` через `try-with-resources`. Переданный в репозиторий `Connection` закрывать не нужно.

## Что сделать

В пакете `learning.task042` находятся два отдельных файла:

- `Player.java` — готовая модель `record Player(int id, String nickname, int level)`;
- `PlayerRepository.java` — класс, который нужно реализовать.

Контракт `PlayerRepository`:

```java
public PlayerRepository(Connection connection)
public Player save(String nickname, int level) throws SQLException
```

- Конструктор сохраняет соединение; для `null` выбрасывает `IllegalArgumentException`.
- Метод проверяет аргументы до запроса: никнейм не равен `null` и не пуст после проверки пробелов, уровень — от 1 до 100 включительно. Для неверных значений выбрасывай `IllegalArgumentException`.
- Сохраняй исходный никнейм без изменения регистра и пробелов.
- Вставляй только `nickname` и `level` через параметризованный `INSERT`. `id` создаёт база.
- Верни `Player` с полученным от базы ID и исходными `nickname`, `level`.
- Ошибки базы, например повтор занятого никнейма, передавай как `SQLException`.

## Примеры

- `save("Kira", 42)` → сохранённый игрок с никнеймом `Kira`, уровнем 42 и новым положительным ID.
- `save(" Newcomer ", 1)` → пробелы в никнейме сохраняются.
- `save("   ", 10)` или `save("Nova", 101)` → `IllegalArgumentException`.

## Ограничения и готовность

- Используй `PreparedStatement` с параметрами и `Statement.RETURN_GENERATED_KEYS`.
- Не вычисляй ID в Java и не передавай его в `INSERT`.
- Закрывай `PreparedStatement` и результат `getGeneratedKeys()`; соединение оставляй открытым.
- Не изменяй модель `Player` и таблицу, создаваемую тестом.
- Тесты проверяют сохранение и выданный базой ID, границы уровня, точность никнейма, неверные аргументы, повторный никнейм и ошибку закрытого соединения.
- Новые зависимости не нужны.

## Файлы

- Модель: `src/main/java/learning/task042/Player.java`
- Решение: `src/main/java/learning/task042/PlayerRepository.java`
- Автотесты: `src/test/java/learning/task042/PlayerRepositoryTest.java`

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
