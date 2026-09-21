# Задание 25 — Разрешение конфликта Git

**Сложность:** следующий шаг Git

## Закрепляем

- ветки, коммиты и merge;
- просмотр состояния через `git status`;
- добавление исправленного файла в staging area.

## Новое понятие: merge conflict

Конфликт появляется, когда `main` и другая ветка изменили одну и ту же часть
файла по-разному. Git не может угадать правильный вариант и останавливает merge.
Ты выбираешь итоговое содержимое, убираешь служебные маркеры и продолжаешь работу.

## Сценарий

В файле [game-balance.txt](game-balance.txt) сейчас есть строка:

```text
rewardMultiplier=1.00
```

Нужно осознанно создать конфликт и оставить в результате вариант из feature-ветки:
`rewardMultiplier=1.20`.

1. Создай ветку и переключись на неё:

   ```bash
   git switch -c task-025-merge-conflict
   ```

2. В этой ветке замени значение в `game-balance.txt` на `1.20`, сделай тематический
   коммит и выполни `git push -u origin task-025-merge-conflict`.
3. Переключись на `main` и в том же файле зафиксируй другое значение: `0.80`.
   Создай отдельный тематический коммит и сделай `git push`.
4. Выполни:

   ```bash
   git merge task-025-merge-conflict
   ```

   Git сообщит о конфликте. Открой `game-balance.txt`: там будут блоки вида
   `<<<<<<<`, `=======`, `>>>>>>>`.
5. Оставь ровно одну строку `rewardMultiplier=1.20`, удали все маркеры конфликта.
   Затем выполни:

   ```bash
   git add tasks/active/task_025_git_merge_conflict/game-balance.txt
   git commit -m "Resolve reward multiplier conflict"
   git push
   ```
6. Заполни [conflict-report.md](conflict-report.md), где покажи `git status -sb`
   после разрешения и коротко объясни, почему Git остановил merge. Создай отдельный
   коммит только с отчётом и выполни `git push`.

Значение `0.80` в `main` здесь создаётся только для учебной ситуации. В обычной
работе изменения сначала делают в отдельной ветке.

## Готово, когда

- в `main` итоговое значение равно `1.20`;
- в файле нет маркеров конфликта;
- отчёт заполнен и закоммичен отдельно;
- `bash ./mvnw test` проходит.

## Файлы

- рабочий файл: `tasks/active/task_025_git_merge_conflict/game-balance.txt`;
- отчёт: `tasks/active/task_025_git_merge_conflict/conflict-report.md`;
- тест: `src/test/java/learning/task025/MergeConflictTest.java`.
