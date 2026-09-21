# Задание 24 — Слияние готовой ветки

**Сложность:** следующий шаг Git

## Закрепляем

- переход между ветками;
- слияние готовой работы в `main`;
- публикацию обновлённой `main`.

## Новое понятие: merge

`git merge <ветка>` добавляет в текущую ветку изменения из указанной ветки.
Здесь текущей веткой должна быть `main`, а в неё нужно добавить готовую
`task-023-git-branch`.

## Что сделать

1. Убедись, что находишься в `task-023-git-branch`: `git branch --show-current`.
2. Переключись на общую ветку: `git switch main`.
3. Слей готовую работу: `git merge --no-ff task-023-git-branch -m "Merge task 023 Git branch"`.
4. Опубликуй обновлённую `main`: `git push`.
5. Заполни [merge-report.md](merge-report.md), затем добавь **только его** в коммит:
   `git add tasks/active/task_024_git_merge/merge-report.md`.
   Создай тематический коммит и выполни `git push`.

`--no-ff` создаёт отдельный merge-коммит. Благодаря ему в истории видно, что
работа велась в отдельной ветке, даже если Git мог бы просто передвинуть указатель.

## Пример ожидаемой истории

```text
* Merge task 023 Git branch       ← main
|\
| * Complete task 023 Git branch  ← task-023-git-branch
| * Document task 023 Git branch workflow
|/
* Complete task 022 Maven profile
```

## Готово, когда

- `main` содержит merge-коммит с `task-023-git-branch`;
- `origin/main` обновлён;
- отчёт заполнен и закоммичен отдельным тематическим коммитом;
- `bash ./mvnw test` завершается успешно.

## Файлы

- отчёт: `tasks/active/task_024_git_merge/merge-report.md`;
- тест: `src/test/java/learning/task024/MergeReportTest.java`.

Запуск тестов: `bash ./mvnw test`.
