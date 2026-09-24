SELECT players.nickname, SUM(player_orders.total_price) AS total_spent
FROM players
LEFT JOIN player_orders ON players.id = player_orders.player_id
GROUP BY players.nickname
HAVING SUM(player_orders.total_price) >= 100
ORDER BY total_spent DESC, players.nickname ASC
