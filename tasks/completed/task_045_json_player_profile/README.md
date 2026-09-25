# Задача №45 — Разбор JSON-профиля игрока

**Сложность:** средняя

**Закрепляем:** `record`, проверку входных данных, исключения и JUnit-тесты.

**Новое:** формат JSON и библиотека Jackson (`ObjectMapper`, `JsonNode`).

В предыдущей задаче HTTP-клиент получил тело ответа как `String`. Теперь преврати такую строку в типизированный профиль игрока. HTTP-запросы здесь повторять не нужно: метод получает уже готовый текст.

## Материал для изучения

JSON хранит данные как объекты (`{}`), массивы (`[]`), строки, числа, логические значения и `null`. В записи `{"id":7,"name":"Лис","level":12}` корень — объект, а `id`, `name` и `level` — его поля. Порядок полей и пробелы не должны влиять на результат; строка `"7"` не равна числу `7`.

В Java 21 нет встроенного удобного API для разбора JSON, поэтому в `pom.xml` добавлен Jackson `jackson-databind` версии 2.22.2. `ObjectMapper.readTree(json)` строит дерево `JsonNode`; у узла можно проверить тип (`isObject()`, `isIntegralNumber()`, `isTextual()`) и получить поле через `path("id")`. Отсутствующее поле даёт специальный missing-узел. Для целого числа проверь `canConvertToInt()` перед `intValue()`, чтобы не потерять переполнение. Неверный JSON вызывает `JsonProcessingException`; в этой задаче преобразуй его в `IllegalArgumentException`, сохранив причину.

Пример изучения одного поля, **не всего решения**:

```java
JsonNode root = mapper.readTree(json);
JsonNode idNode = root.path("id");
boolean validType = idNode.isIntegralNumber() && idNode.canConvertToInt();
```

Дополнительно: [документация Jackson](https://github.com/FasterXML/jackson-databind) и [заметки о выпуске 2.22.2](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.22.2).

## Что сделать

Создай два самостоятельных файла в пакете `learning.task045`:

- `PlayerProfile.java` — `public record PlayerProfile(int id, String name, int level) {}`. Запись только хранит данные; дополнительную логику в ней не делай.
- `PlayerProfileParser.java` — класс с методом `public PlayerProfile parse(String json)`.

Требования к `parse`:

- Корень JSON должен быть объектом. Поля `id`, `name`, `level` обязательны; остальные поля игнорируй.
- `id` — положительное целое число, помещающееся в Java `int`.
- `name` — JSON-строка, не пустая и не состоящая только из пробелов. Не обрезай её и не меняй регистр.
- `level` — целое число от 1 до 100 включительно.
- Для `null`, пустого текста, синтаксической ошибки, отсутствующего или неверного поля выбрасывай `IllegalArgumentException`. Для синтаксической ошибки сохрани исходное исключение как `cause`.
- Не разбирай JSON вручную через `split`, регулярные выражения или поиск подстрок.

## Примеры

- `{"id":7,"name":"Лис","level":12}` → `new PlayerProfile(7, "Лис", 12)`.
- `{ "level": 1, "name": " Лис ", "id": 1, "online": true }` → `new PlayerProfile(1, " Лис ", 1)`.
- `{"id":"7","name":"Лис","level":12}` → `IllegalArgumentException`: `id` — строка, а не число.

## Ограничения и готовность

- Используй только добавленную зависимость Jackson; модель оставь отдельным `record`.
- Тесты проверяют обычный разбор, порядок полей, лишние поля, границы, неверные типы, пропущенные поля, переполнение и синтаксические ошибки.
- Сначала реализуй проверку корня, затем поля по одному: так ошибки легче локализовать.

## Файлы

- Решение: `src/main/java/learning/task045/PlayerProfile.java` и `src/main/java/learning/task045/PlayerProfileParser.java`.
- Автотесты: `src/test/java/learning/task045/PlayerProfileParserTest.java`.

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
