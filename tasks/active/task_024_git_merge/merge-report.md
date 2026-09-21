# Task 024 — Git merge

<!-- Заполни после слияния. -->

## `git status -sb` после merge

M .idea/misc.xml
?? .idea/vcs.xml
?? src/test/java/learning/
?? tasks/active/task_024_git_merge/


## Фрагмент `git log --oneline --decorate -4`

kriksson@kriksson:~/IdeaProjects/javaLearnAI$ git log --oneline --decorate -4
2cacf68 (HEAD -> main, origin/main, origin/HEAD) MErge task 023 git branch
f0ebc8d (origin/task-023-git-branch, task-023-git-branch) Complete task 023 Git branch
139763a Document task 023 Git branch workflow
0936085 Complete task 022 Maven profile


## Ответ

Почему после `git switch main` команда `git push` не требует `-u`?

Потому что ранее уже произошла связка и main связан с origin/main
