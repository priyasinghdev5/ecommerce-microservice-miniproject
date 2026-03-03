CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE payments (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id         UUID NOT NULL UNIQUE,
    user_id          UUID NOT NULL,
    amount           NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency         CHAR(3) NOT NULL DEFAULT 'INR',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method   VARCHAR(50) NOT NULL,
    gateway          VARCHAR(50) NOT NULL DEFAULT 'MOCK_GATEWAY',
    gateway_ref      VARCHAR(255),
    gateway_response TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id  UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    gateway_ref VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order  ON payments(order_id);
CREATE INDEX idx_payments_user   ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_txn_payment     ON transactions(payment_id);
