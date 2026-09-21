# Задача №23: Git-ветка для изменения

- Сложность: высокая, Git и GitHub
- Закрепление: статус репозитория, staging, commit и push
- Новые понятия: ветка Git, изолированное изменение и публикация ветки

## Контекст

Ветки позволяют выполнять изменение отдельно от `main`. Пока работа не готова,
основная ветка остаётся стабильной. Для одной задачи обычно создают одну
тематическую ветку.

## Задание

В корне проекта выполни команды:

```shell
git switch -c task-023-git-branch
git status -sb
```

Создай или заполни `branch-report.md` в этом каталоге. Он должен содержать
четыре коротких пункта:

```markdown
# Отчёт о Git-ветке

- Имя ветки: task-023-git-branch
- Проверка текущей ветки: git status -sb
- Добавление файла в staging: git add tasks/active/task_023_git_branch/branch-report.md
- Зачем нужна ветка: ...
```

Затем добавь **только** отчёт в staging, создай коммит и отправь ветку:

```shell
git add tasks/active/task_023_git_branch/branch-report.md
git commit -m "Document task 023 Git branch workflow"
git push -u origin task-023-git-branch
```

Не добавляй файлы `.idea` в коммит.

## Как проверить себя

```shell
git status -sb
git log -1 --oneline
git branch -vv
```

В выводе должны быть текущая ветка `task-023-git-branch`, твой коммит и связь
с удалённой веткой `origin/task-023-git-branch`.

## Автоматическая проверка

`BranchReportTest` проверяет содержание отчёта. При проверке задачи я также
проверю текущую ветку, коммит и её публикацию на GitHub.

## Ограничения и критерии готовности

- работай в ветке `task-023-git-branch`, созданной от `main`;
- в твоём коммите есть только `branch-report.md` из этой задачи;
- ветка опубликована через `git push -u`;
- не редактируй `BranchReportTest`;
- `./mvnw test` проходит.

## Файлы

- Твой отчёт: `tasks/active/task_023_git_branch/branch-report.md`;
- Тест: `src/test/java/learning/task023/BranchReportTest.java`.

## Команды

Linux/macOS:

```shell
./mvnw test
```

Windows:

```shell
.\mvnw.cmd test
```
