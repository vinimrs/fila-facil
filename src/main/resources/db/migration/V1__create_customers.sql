CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    document    VARCHAR(30)  NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    email       VARCHAR(200),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL
);
