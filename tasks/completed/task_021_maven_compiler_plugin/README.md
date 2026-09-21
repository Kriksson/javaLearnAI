# Задача №21: Maven Compiler Plugin

- Сложность: высокая, Maven
- Закрепление: `pom.xml`, Maven Wrapper и фазы сборки
- Новое понятие: Maven-плагин и его конфигурация

## Контекст

Свойства Maven задают общие значения, а плагины выполняют конкретную работу.
Например, `maven-compiler-plugin` компилирует исходный код Java. Явная настройка
фиксирует, какую версию Java должен использовать проект на любой машине и в CI.

## Задание

В разделе `<build><plugins>` файла `pom.xml` добавь плагин компилятора:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>${maven.compiler.release}</release>
    </configuration>
</plugin>
```

Свойство `maven.compiler.release` уже равно `21`; не дублируй это число в
конфигурации плагина.

После изменения выполни:

```shell
./mvnw clean test
./mvnw help:effective-pom -Doutput=target/effective-pom.xml
```

Вторая команда создаёт полный «эффективный POM»: Maven объединяет твой `pom.xml`
с настройками по умолчанию и выводит итоговую конфигурацию.

Создай файл `plugin-report.md` в каталоге задачи:

```markdown
# Отчёт о Compiler Plugin

- Назначение плагина: ...
- Версия плагина: ...
- Откуда берётся версия Java: ...
- Путь к effective POM: ...
```

## Автоматическая проверка

`CompilerPluginTest` проверяет, что в `pom.xml` есть нужный плагин, версия и
ссылка на свойство Java 21.

## Ограничения и критерии готовности

- не меняй существующие Maven-координаты, JUnit и Surefire;
- не добавляй новых зависимостей;
- версия Java указывается через `${maven.compiler.release}`;
- не редактируй `CompilerPluginTest`;
- `./mvnw clean test` завершается с `BUILD SUCCESS`;
- создай `plugin-report.md`.

## Файлы

- Конфигурация: `pom.xml`;
- Тест: `src/test/java/learning/task021/CompilerPluginTest.java`;
- Твой отчёт: `tasks/active/task_021_maven_compiler_plugin/plugin-report.md`.

## Команды

Linux/macOS:

```shell
./mvnw clean test
./mvnw help:effective-pom -Doutput=target/effective-pom.xml
```

Windows:

```shell
.\mvnw.cmd clean test
.\mvnw.cmd help:effective-pom -Doutput=target/effective-pom.xml
```
