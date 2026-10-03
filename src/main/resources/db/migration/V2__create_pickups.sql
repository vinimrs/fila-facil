CREATE TABLE pickups (
    id                   BIGSERIAL PRIMARY KEY,
    customer_id          BIGINT       NOT NULL REFERENCES customers (id),
    region               VARCHAR(20)  NOT NULL,
    address              VARCHAR(300) NOT NULL,
    package_count        INT          NOT NULL,
    manual_priority      INT,
    status               VARCHAR(20)  NOT NULL,
    priority             INT          NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    rescheduled_from_id  BIGINT,
    proof_path           VARCHAR(500)
);
