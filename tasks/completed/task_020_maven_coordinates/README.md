# Задача №20: Maven-координаты и сборка JAR

- Сложность: высокая, новый этап Maven
- Закрепление: Maven Wrapper, жизненный цикл сборки и JUnit 5
- Новые понятия: Maven-координаты `groupId`, `artifactId`, `version`; фазы `clean`, `test`, `package`

## Контекст

Maven идентифицирует каждый проект тремя координатами:

```text
groupId:artifactId:version
```

Они определяют имя собираемого JAR и помогают другим проектам подключать твою
библиотеку как зависимость.

## Задание

Измени координаты в корневом `pom.xml`:

```xml
<groupId>ru.kriksson.javacourse</groupId>
<artifactId>java-learning</artifactId>
<version>1.0.0</version>
```

Не меняй Java 21, кодировку, JUnit-зависимость и настройки Surefire.

После изменения выполни:

```shell
./mvnw clean package
```

Команда последовательно:

1. `clean` удаляет предыдущий каталог `target`;
2. запускает компиляцию и все тесты;
3. создаёт JAR `target/java-learning-1.0.0.jar`.

Создай в каталоге этой задачи файл `build-report.md` со следующими ответами:

```markdown
# Отчёт о Maven-сборке

- Координаты проекта: ...
- Команда сборки: ...
- Путь к созданному JAR: ...
- Что проверяет фаза `test`: ...
```

Напиши ответы своими словами, по одной короткой строке на пункт.

## Автоматическая проверка

Тест `PomCoordinatesTest` проверяет Maven-координаты в `pom.xml`. Перед
завершением отдельно запусти полную сборку `./mvnw clean package`.

## Ограничения и критерии готовности

- меняй только три Maven-координаты в `pom.xml`;
- не редактируй тест `PomCoordinatesTest`;
- создай `build-report.md`;
- `./mvnw clean package` завершается с `BUILD SUCCESS`;
- в `target` существует JAR с ожидаемым именем.

## Файлы

- Конфигурация: `pom.xml`;
- Тест: `src/test/java/learning/task020/PomCoordinatesTest.java`;
- Твой отчёт: `tasks/active/task_020_maven_coordinates/build-report.md`.

## Команды

Linux/macOS:

```shell
./mvnw test
./mvnw clean package
```

Windows:

```shell
.\mvnw.cmd test
.\mvnw.cmd clean package
```
