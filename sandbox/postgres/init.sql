\c users_localdev;

CREATE SCHEMA IF NOT EXISTS users;

ALTER DATABASE users_localdev SET search_path TO users, public;

ALTER ROLE postgres SET search_path TO users, public;