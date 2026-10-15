CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    customer_id      VARCHAR(64)    NOT NULL,
    idempotency_key  VARCHAR(100)   NOT NULL UNIQUE,
    status           VARCHAR(32)    NOT NULL,
    total_amount     NUMERIC(12, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    failure_reason   VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    version          BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_customer ON orders (customer_id, created_at DESC);

CREATE TABLE order_lines (
    order_id    UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    sku         VARCHAR(64)    NOT NULL,
    quantity    INT            NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_lines_order ON order_lines (order_id);

CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    aggregate_id  VARCHAR(64)  NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    topic         VARCHAR(128) NOT NULL,
    payload       TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    published_at  TIMESTAMPTZ
);

-- Partial index keeps the relay's "unpublished" scan cheap as the table grows.
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
