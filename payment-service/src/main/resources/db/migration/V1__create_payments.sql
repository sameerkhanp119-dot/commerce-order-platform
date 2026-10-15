CREATE TABLE payments (
    id                 UUID PRIMARY KEY,
    order_id           UUID           NOT NULL UNIQUE,
    customer_id        VARCHAR(64)    NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    status             VARCHAR(32)    NOT NULL,
    gateway_reference  VARCHAR(128),
    decline_reason     VARCHAR(255),
    created_at         TIMESTAMPTZ    NOT NULL
);

CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    aggregate_id  VARCHAR(64)  NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    topic         VARCHAR(128) NOT NULL,
    payload       TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    published_at  TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
