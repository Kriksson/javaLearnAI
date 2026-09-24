-- Запрос 1: игроки уровня 40 и выше.
SELECT nickname, level
FROM players
WHERE level >= 40
ORDER BY level DESC, nickname ASC;

-- Запрос 2: заказы игрока с id 1.
SELECT item_name, total_price
FROM player_orders
WHERE player_id = 1
ORDER BY total_price DESC, item_name ASC;

-- Запрос 3: сумма заказов по игрокам.
SELECT player_id, SUM(total_price) AS total_spent
FROM player_orders
GROUP BY player_id
ORDER BY total_spent DESC, player_id ASC;
