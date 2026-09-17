CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED',
    'REFUNDED'
);

CREATE TABLE payments
(
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    order_id     UUID           NOT NULL,
    status       payment_status NOT NULL DEFAULT 'PENDING',
    amount       NUMERIC(12, 2) NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
);
