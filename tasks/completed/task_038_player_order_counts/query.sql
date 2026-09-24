SELECT players.nickname, COUNT(player_orders.item_name) as order_count
FROM players
LEFT JOIN player_orders ON players.id = player_orders.player_id
GROUP BY players.nickname
ORDER BY order_count DESC, players.nickname ASC;
