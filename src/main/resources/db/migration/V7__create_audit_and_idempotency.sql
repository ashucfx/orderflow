CREATE TABLE audit_logs
(
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    actor_id    UUID,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   VARCHAR(100) NOT NULL,
    metadata    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_actor_id_created_at ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);

CREATE TABLE idempotency_keys
(
    key           VARCHAR(64) NOT NULL,
    response_body TEXT        NOT NULL,
    status_code   INT         NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (key)
);
