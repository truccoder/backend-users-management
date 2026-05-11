CREATE TABLE users.t_users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    full_name VARCHAR(255),
    profile_picture_url VARCHAR(1024),
    created_at TIMESTAMP with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP with time zone
);

-----------

CREATE TABLE users.t_refresh_tokens (
    token VARCHAR(255),
    user_id VARCHAR(255),
    expires_at TIMESTAMP with time zone,
    created_at TIMESTAMP with time zone DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX t_refresh_tokens_idx_refresh_token ON users.t_refresh_tokens USING HASH(token);
CREATE INDEX t_refresh_tokens_idx_user_id ON users.t_refresh_tokens(user_id);

-----------

CREATE TABLE users.t_friend_requests (
    id VARCHAR(255) PRIMARY KEY,
    requester_id VARCHAR(255),
    addressee_id VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP with time zone
);
CREATE INDEX t_friend_requests_idx_requester_id ON users.t_friend_requests(requester_id);
CREATE INDEX t_friend_requests_idx_addressee_id ON users.t_friend_requests(addressee_id);

-----------

CREATE TABLE users.t_password_reset_tokens (
   token VARCHAR(255) PRIMARY KEY,
   user_id VARCHAR(255),
   expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX t_password_reset_tokens_idx_token ON users.t_password_reset_tokens USING HASH(token);
