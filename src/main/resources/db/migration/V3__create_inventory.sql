CREATE TABLE inventory
(
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    product_id         UUID        NOT NULL,
    available_quantity INT         NOT NULL DEFAULT 0,
    reserved_quantity  INT         NOT NULL DEFAULT 0,
    sold_quantity      INT         NOT NULL DEFAULT 0,
    version            BIGINT      NOT NULL DEFAULT 0,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_inventory_available CHECK (available_quantity >= 0),
    CONSTRAINT chk_inventory_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_sold CHECK (sold_quantity >= 0)
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);
