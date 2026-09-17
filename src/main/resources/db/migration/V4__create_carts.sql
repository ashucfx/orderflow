CREATE TABLE carts
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    cart_id    UUID        NOT NULL,
    product_id UUID        NOT NULL,
    quantity   INT         NOT NULL,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0)
);
