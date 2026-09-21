# Задача №22: Maven-профиль development

- Сложность: высокая, Maven
- Закрепление: `pom.xml`, Maven-плагины и команды Maven Wrapper
- Новое понятие: Maven-профиль и свойства окружения

## Контекст

Один проект часто запускают в разных окружениях: на компьютере разработчика,
на тестовом сервере и в production. Maven-профили позволяют включать набор
настроек по имени, не меняя основной POM.

## Задание

Добавь в корневой `pom.xml` раздел `<profiles>` с профилем:

```xml
<profile>
    <id>development</id>
    <properties>
        <app.environment>dev</app.environment>
    </properties>
</profile>
```

Размещай `<profiles>` на том же уровне, что и `<build>` — непосредственно внутри
корневого элемента `<project>`.

Проверь профиль командами:

```shell
./mvnw help:active-profiles -Pdevelopment
./mvnw help:evaluate -Dexpression=app.environment -Pdevelopment -q -DforceStdout
./mvnw clean test
```

Вторая команда должна вывести `dev`.

Создай в каталоге задачи файл `profile-report.md`:

```markdown
# Отчёт о Maven-профиле

- Имя профиля: ...
- Свойство профиля: ...
- Как включить профиль: ...
- Результат `help:evaluate`: ...
```

## Автоматическая проверка

`DevelopmentProfileTest` проверяет, что профиль и свойство корректно записаны
в `pom.xml`.

## Ограничения и критерии готовности

- не меняй существующие Maven-координаты, плагины, Java 21 и зависимости;
- не активируй профиль по умолчанию;
- не редактируй `DevelopmentProfileTest`;
- создай `profile-report.md`;
- `./mvnw clean test` завершается с `BUILD SUCCESS`.

## Файлы

- Конфигурация: `pom.xml`;
- Тест: `src/test/java/learning/task022/DevelopmentProfileTest.java`;
- Твой отчёт: `tasks/active/task_022_maven_profile/profile-report.md`.

## Команды

Linux/macOS:

```shell
./mvnw help:active-profiles -Pdevelopment
./mvnw help:evaluate -Dexpression=app.environment -Pdevelopment -q -DforceStdout
./mvnw clean test
```

Windows:

```shell
.\mvnw.cmd help:active-profiles -Pdevelopment
.\mvnw.cmd help:evaluate -Dexpression=app.environment -Pdevelopment -q -DforceStdout
.\mvnw.cmd clean test
```
