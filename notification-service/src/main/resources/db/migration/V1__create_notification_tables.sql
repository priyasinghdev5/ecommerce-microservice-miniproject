CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE notification_templates (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    channel     VARCHAR(20) NOT NULL,
    subject     VARCHAR(255),
    body        TEXT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID NOT NULL,
    channel       VARCHAR(20) NOT NULL,
    recipient     VARCHAR(255) NOT NULL,
    subject       VARCHAR(255),
    body          TEXT NOT NULL,
    template_name VARCHAR(100),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts      SMALLINT NOT NULL DEFAULT 0,
    reference_id  VARCHAR(255),
    error_message TEXT,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user   ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_channel ON notifications(channel);
