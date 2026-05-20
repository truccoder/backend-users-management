ALTER TABLE users.t_users DROP COLUMN IF EXISTS password;

DROP TABLE IF EXISTS users.t_refresh_tokens;
DROP TABLE IF EXISTS users.t_password_reset_tokens;
