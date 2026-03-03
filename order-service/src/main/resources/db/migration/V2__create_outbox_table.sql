CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    topic          VARCHAR(200) NOT NULL,
    payload        TEXT NOT NULL,
    status         VARCHAR(10) NOT NULL DEFAULT 'NEW',
    retry_count    SMALLINT NOT NULL DEFAULT 0,
    error_message  TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMPTZ
);

-- Partial index: scheduler only scans NEW/FAILED with retries remaining
CREATE INDEX idx_outbox_pending ON outbox_events(created_at ASC)
    WHERE status IN ('NEW', 'FAILED') AND retry_count < 3;
