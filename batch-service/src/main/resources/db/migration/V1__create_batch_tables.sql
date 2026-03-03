CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE import_jobs (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename         VARCHAR(255) NOT NULL,
    source_path      VARCHAR(500) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_records    INT NOT NULL DEFAULT 0,
    processed_count  INT NOT NULL DEFAULT 0,
    error_count      INT NOT NULL DEFAULT 0,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE import_errors (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id       UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number   INT,
    raw_data     TEXT,
    error_msg    TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_import_jobs_status ON import_jobs(status);
CREATE INDEX idx_import_errors_job  ON import_errors(job_id);
