CREATE TABLE guild_recruits (
    id BIGINT PRIMARY KEY,
    name VARCHAR(30),
    level INTEGER
);
INSERT INTO guild_recruits (id, name, level) VALUES (10, 'Лира', 25);
INSERT INTO guild_recruits (id, name, level) VALUES (20, 'Кирилл', 1);
INSERT INTO guild_recruits (id, name, level) VALUES (30, 'Мира', 100);