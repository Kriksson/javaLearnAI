# Задача №40 — Обновление статусов заказов

**Сложность:** SQL, начинающий уровень

**Закрепляем:** `WHERE`, таблицы и ограничения.

**Новое:** изменение строк через `UPDATE` и удаление через `DELETE`.

Магазин получил оплату заказа №101 и должен удалить отменённые заказы. В `src/main/resources/learning/task040/changes.sql` напиши две SQL-команды.

## Материал для изучения

`UPDATE` меняет значения в существующих строках. `SET` задаёт новое значение, а `WHERE` выбирает нужные строки:

```sql
UPDATE orders
SET status = 'PAID'
WHERE id = 42 AND status = 'PENDING';
```

`DELETE` удаляет строки, подходящие под условие:

```sql
DELETE FROM orders
WHERE status = 'CANCELLED';
```

У `UPDATE` и `DELETE` без `WHERE` действие применяется ко всем строкам таблицы. Перед запуском полезно проверить условие отдельным `SELECT ... WHERE ...`, чтобы убедиться, какие строки оно выберет.

## Что сделать

Напиши команды в таком порядке:

1. Обнови статус заказа с `id = 101` на `PAID`, только если его текущий статус `PENDING`.
2. Удали все заказы со статусом `CANCELLED`.

Таблица называется `player_orders`, поля — `id`, `player_id`, `item_name`, `status`.

## Данные теста

| id | item_name | status | Результат |
|---:|---|---|---|
| 101 | Magic sword | PENDING | статус станет `PAID` |
| 102 | Bow | PAID | останется без изменений |
| 103 | Potion | CANCELLED | строка будет удалена |
| 104 | Shield | PENDING | останется без изменений |
| 105 | Dragon armor | CANCELLED | строка будет удалена |

## Ограничения и готовность

- В файле должны быть ровно два DML-запроса: один `UPDATE` и один `DELETE`.
- Условия должны затронуть только указанный неоплаченный заказ и отменённые заказы.
- Тест проверяет число изменённых и удалённых строк, итоговые статусы и сохранность остальных заказов.
- Не добавляй Java-код и зависимости.

## Файлы

- Решение: `src/main/resources/learning/task040/changes.sql`
- Автотест: `src/test/java/learning/task040/OrderStateChangesTest.java`

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
