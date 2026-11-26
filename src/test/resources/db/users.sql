CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    full_name  VARCHAR(255) NOT NULL,
    role       VARCHAR(32)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login DATE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);
