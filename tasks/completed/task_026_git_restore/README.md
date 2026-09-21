# Задание 26 — Отмена локального изменения Git

**Сложность:** следующий шаг Git

## Закрепляем

- просмотр рабочего дерева через `git status`;
- чтение различий через `git diff`;
- отдельный тематический коммит и push.

## Новое понятие: `git restore`

`git restore <файл>` отменяет **незакоммиченные** изменения указанного файла и
возвращает версию из последнего коммита (`HEAD`). Команда не удаляет коммиты и
не затрагивает остальные файлы.

## Что сделать

1. Открой `pom.xml` и временно измени версию проекта:

   ```xml
   <version>1.0.0</version>
   ```

   на

   ```xml
   <version>1.0.1-SNAPSHOT</version>
   ```

2. Выполни и посмотри разницу:

   ```bash
   git status -sb
   git diff -- pom.xml
   ```

3. Отмени только это изменение:

   ```bash
   git restore pom.xml
   ```

4. Проверь результат:

   ```bash
   git diff --exit-code -- pom.xml
   ```

   Команда не должна ничего вывести и должна завершиться успешно. Убедись, что
   в `pom.xml` снова версия `1.0.0`.

5. Заполни [restore-report.md](restore-report.md): кратко опиши назначение
   `git diff` и `git restore`, а также вставь результат финальной проверки.
   Добавь в коммит только отчёт и выполни `git push`.

## Пример

```text
git diff -- pom.xml
-  <version>1.0.0</version>
+  <version>1.0.1-SNAPSHOT</version>
```

После `git restore pom.xml` этой разницы больше нет.

## Готово, когда

- `pom.xml` снова содержит версию `1.0.0`;
- `git diff --exit-code -- pom.xml` завершается успешно;
- отчёт заполнен, закоммичен отдельно и опубликован;
- `bash ./mvnw test` проходит.

## Файлы

- отчёт: `tasks/active/task_026_git_restore/restore-report.md`;
- тест: `src/test/java/learning/task026/GitRestoreReportTest.java`.
