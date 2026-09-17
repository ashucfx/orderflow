CREATE TYPE order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

CREATE TABLE orders
(
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL,
    status           order_status   NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(12, 2) NOT NULL,
    idempotency_key  VARCHAR(64),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_idempotency_key ON orders (idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE order_items
(
    id          UUID           NOT NULL DEFAULT gen_random_uuid(),
    order_id    UUID           NOT NULL,
    product_id  UUID           NOT NULL,
    quantity    INT            NOT NULL,
    unit_price  NUMERIC(10, 2) NOT NULL,
    subtotal    NUMERIC(12, 2) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0)
);
