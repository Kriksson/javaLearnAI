UPDATE player_orders
SET status = 'PAID'
WHERE id = 101 and status = 'PENDING';

DELETE FROM player_orders
WHERE status = 'CANCELLED';