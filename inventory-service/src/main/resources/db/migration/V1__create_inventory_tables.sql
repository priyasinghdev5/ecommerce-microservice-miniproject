CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE inventory (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id      UUID NOT NULL,
    sku             VARCHAR(100) NOT NULL UNIQUE,
    total_quantity  INT NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    reserved_qty    INT NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    reorder_point   INT NOT NULL DEFAULT 10,
    warehouse_loc   VARCHAR(100),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reserved_not_exceed CHECK (reserved_qty <= total_quantity)
);

CREATE TABLE stock_reservations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL,
    inventory_id    UUID NOT NULL REFERENCES inventory(id),
    sku             VARCHAR(100) NOT NULL,
    quantity        INT NOT NULL CHECK (quantity > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '30 minutes',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_sku        ON inventory(sku);
CREATE INDEX idx_inventory_product    ON inventory(product_id);
CREATE INDEX idx_reservations_order   ON stock_reservations(order_id);
CREATE INDEX idx_reservations_status  ON stock_reservations(status) WHERE status = 'RESERVED';
CREATE INDEX idx_reservations_expires ON stock_reservations(expires_at) WHERE status = 'RESERVED';
