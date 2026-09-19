# Задача №8: файловый рейтинг игроков

- Сложность: высокая
- Закрепление: отдельные классы, списки, циклы, строки, проверка формата и исключения
- Новые навыки: `Path`, `Files`, UTF-8, `IOException`, запись и чтение файлов

## Контекст

Класс `ScoreEntry` уже готов. Он хранит имя игрока и количество очков. Изменять его не требуется.

Реализуй `LeaderboardFileRepository`, который сохраняет список результатов в файл и загружает его обратно.

## Формат файла

Один результат занимает одну строку:

```text
имя;очки
```

Пример:

```text
Kirill;1500
Alex;920
Player One;700
```

Имена в `ScoreEntry` не содержат `;`, `\n` и `\r`.

## Методы

```java
public void save(Path path, List<ScoreEntry> entries) throws IOException
public List<ScoreEntry> load(Path path) throws IOException
```

### `save`

- Преобразует каждый объект в строку `имя;очки`.
- Сохраняет строки в UTF-8, сохраняя порядок списка.
- Перезаписывает существующий файл.
- Если родительского каталога нет, создаёт его.
- Пустой список создаёт пустой файл.

### `load`

- Если файла нет, возвращает пустой изменяемый `ArrayList`.
- Читает файл в UTF-8.
- Игнорирует полностью пустые строки.
- Сохраняет порядок результатов.
- Каждая непустая строка должна содержать ровно один символ `;`.
- Имя после `trim()` не может быть пустым.
- Очки должны быть целым неотрицательным числом.
- При неверном формате выбрасывает `IllegalArgumentException`.

## Пример

```java
List<ScoreEntry> entries = List.of(
        new ScoreEntry("Kirill", 1500),
        new ScoreEntry("Alex", 920)
);

repository.save(path, entries);
List<ScoreEntry> loaded = repository.load(path);
```

Содержимое файла:

```text
Kirill;1500
Alex;920
```

`loaded` должен содержать два равных объекта в том же порядке.

## Ограничения

- `ScoreEntry` и `LeaderboardFileRepository` находятся в отдельных файлах.
- Используй методы класса `Files` и `StandardCharsets.UTF_8`.
- Не проглатывай `IOException`: методы объявляют `throws IOException`.
- Не меняй публичные сигнатуры.

## Критерии готовности

- Список корректно проходит полный цикл `save → load`.
- Каталоги создаются автоматически.
- Отсутствующий и пустой файл обрабатываются корректно.
- Повреждённые строки вызывают `IllegalArgumentException`.
- Все тесты проходят.

## Файлы

- Архив решения: `LeaderboardFileRepository.java`
- Готовый класс: `ScoreEntry.java`
- Архив тестов: `LeaderboardFileRepositoryTest.java`

## Запуск тестов

Windows:

```shell
.\mvnw.cmd test
```

Linux/macOS:

```shell
./mvnw test
```
