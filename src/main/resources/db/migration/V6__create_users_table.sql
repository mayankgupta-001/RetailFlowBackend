CREATE TABLE app_users (
                           id             BIGSERIAL PRIMARY KEY,
                           username       VARCHAR(255) NOT NULL UNIQUE,
                           password_hash  VARCHAR(255) NOT NULL,
                           role           VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'CASHIER'))
);