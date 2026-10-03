CREATE TABLE manifests (
    id           BIGSERIAL PRIMARY KEY,
    route_code   VARCHAR(30),
    uploaded_by  VARCHAR(100),
    content      TEXT      NOT NULL,
    created_at   TIMESTAMP NOT NULL
);
