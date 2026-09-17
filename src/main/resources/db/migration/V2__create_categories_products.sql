CREATE TABLE categories
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE products
(
    id          UUID           NOT NULL DEFAULT gen_random_uuid(),
    category_id UUID           NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    description TEXT,
    sku         VARCHAR(100)   NOT NULL,
    unit_price  NUMERIC(10, 2) NOT NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_active_name ON products (active, name);
