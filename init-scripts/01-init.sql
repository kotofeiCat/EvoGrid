SELECT 'CREATE DATABASE user_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_db')\gexec
SELECT 'CREATE DATABASE pokemon_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pokemon_db')\gexec
SELECT 'CREATE DATABASE world_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'world_db')\gexec
SELECT 'CREATE DATABASE battle_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'battle_db')\gexec
SELECT 'CREATE DATABASE inventory_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')\gexec
