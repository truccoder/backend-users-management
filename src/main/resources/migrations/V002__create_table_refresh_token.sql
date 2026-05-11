DROP TABLE users.t_refresh_tokens;

CREATE TABLE users.t_refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    expires_at TIMESTAMP with time zone
);
CREATE INDEX t_refresh_tokens_idx_refresh_token ON users.t_refresh_tokens USING HASH(token);
CREATE INDEX t_refresh_tokens_idx_user_id ON users.t_refresh_tokens(user_id);