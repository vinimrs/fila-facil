CREATE TABLE deliveries (
    id                   BIGSERIAL PRIMARY KEY,
    tracking_code        VARCHAR(30),
    customer_name        VARCHAR(200),
    customer_document    VARCHAR(30),
    customer_email       VARCHAR(200),
    driver_name          VARCHAR(200),
    driver_phone         VARCHAR(30),
    vehicle_plate        VARCHAR(10),
    region               VARCHAR(20),
    destination_address  VARCHAR(300),
    status               VARCHAR(20),
    freight_value        NUMERIC(10, 2),
    refunded_amount      NUMERIC(10, 2) DEFAULT 0,
    scheduled_date       DATE,
    delivered_at         TIMESTAMP,
    occurrence_type      VARCHAR(50),
    occurrence_notes     TEXT,
    occurrence_at        TIMESTAMP
);

CREATE TABLE refunds (
    id            BIGSERIAL PRIMARY KEY,
    delivery_id   BIGINT,
    reason        VARCHAR(50),
    amount        NUMERIC(10, 2),
    status        VARCHAR(20),
    requested_by  VARCHAR(100),
    requested_at  TIMESTAMP,
    approved_by   VARCHAR(100),
    approved_at   TIMESTAMP,
    notes         TEXT
);

CREATE TABLE payments (
    id         BIGSERIAL PRIMARY KEY,
    refund_id  BIGINT,
    amount     NUMERIC(10, 2),
    paid_at    TIMESTAMP
);
