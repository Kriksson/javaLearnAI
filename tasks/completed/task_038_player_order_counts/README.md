 # Задача №38 — Подсчёт заказов игроков

**Сложность:** SQL, начинающий уровень

**Закрепляем:** `SELECT`, агрегаты, `GROUP BY`, сортировку.

**Новое:** объединение таблиц через `JOIN` и сохранение строк без пары через `LEFT JOIN`.

Нужен отчёт по всем игрокам и количеству их заказов. В `src/main/resources/learning/task038/query.sql` напиши один SQL-запрос.

## Материал для изучения

`JOIN` связывает строки двух таблиц по условию. Например, заказ связан с игроком по `players.id = player_orders.player_id`:

```sql
SELECT players.nickname, player_orders.item_name
FROM players
JOIN player_orders ON players.id = player_orders.player_id;
```

Обычный `JOIN` (синоним `INNER JOIN`) возвращает только строки, для которых нашлась пара в обеих таблицах. `LEFT JOIN` сохраняет все строки слева, даже если справа пары нет; поля правой таблицы в таком случае содержат `NULL`.

```sql
SELECT players.nickname, player_orders.id
FROM players
LEFT JOIN player_orders ON players.id = player_orders.player_id;
```

`COUNT(поле)` считает только значения, которые не равны `NULL`. Поэтому при подсчёте строк справа от `LEFT JOIN` считай её идентификатор, например `COUNT(player_orders.id)`. `COUNT(*)` посчитает и сохранённую строку игрока без заказа.

## Что сделать

Выведи две колонки:

- `nickname` — имя игрока;
- `order_count` — количество его заказов.

Включи в отчёт всех игроков, в том числе без заказов. Отсортируй по убыванию числа заказов, а при равенстве — по `nickname` по возрастанию.

## Данные теста

| Игрок | Заказы | Ожидаемое число |
|---|---|---:|
| Kira | Magic sword, Bow | 2 |
| Milo | Shield | 1 |
| Nova | Dragon armor | 1 |
| Zed | нет | 0 |

Ожидаемый порядок: Kira, Milo, Nova, Zed.

## Ограничения и готовность

- Напиши один запрос `SELECT`; таблицы и тестовые данные создаёт тест.
- Используй таблицы `players` и `player_orders` и связывай их по идентификаторам.
- Сохрани в результате игроков без заказов и выведи счётчик под именем `order_count`.
- Тест проверяет количество у каждого игрока, нулевой случай и порядок при равных значениях.
- Не добавляй Java-код и зависимости.

## Файлы

- Решение: `src/main/resources/learning/task038/query.sql`
- Автотест: `src/test/java/learning/task038/PlayerOrderCountsTest.java`

## Запуск

```shell
JAVA_HOME=/home/kriksson/.jdks/temurin-24.0.2 PATH=/home/kriksson/.jdks/temurin-24.0.2/bin:$PATH bash ./mvnw clean test
```
