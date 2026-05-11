DROP TABLE users.t_password_reset_tokens;

CREATE TABLE users.t_password_reset_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    expires_at TIMESTAMP with time zone
);
CREATE INDEX t_password_reset_tokens_idx_token ON users.t_password_reset_tokens USING HASH(token);