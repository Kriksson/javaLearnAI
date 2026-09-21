# Task 025 — Git merge conflict

## `git status -sb` после разрешения

kriksson@kriksson:~/IdeaProjects/javaLearnAI$ git status -sb
## main...origin/main
M .idea/misc.xml
M README.md
?? .idea/vcs.xml
?? src/test/java/learning/
?? tasks/active/task_025_git_merge_conflict/README.md
?? tasks/active/task_025_git_merge_conflict/conflict-report.md
kriksson@kriksson:~/IdeaProjects/javaLearnAI$


## Почему Git остановил merge

Git остановил merge, потому что две ветки изменили одну и ту же строку файла разными значениями.
