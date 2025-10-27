-- ===== SCHEMA FOR DOCUMENTS & PROTOCOLS =====

-- ========== DOCUMENTS ==========
CREATE TABLE IF NOT EXISTS "document"
(
    id           BIGSERIAL PRIMARY KEY,
    public_id    UUID        NOT NULL UNIQUE DEFAULT uuid_generate_v4(),

    filename     TEXT        NOT NULL,
    content_type TEXT        NOT NULL,
    size_bytes   BIGINT      NOT NULL CHECK (size_bytes >= 0),

    storage_path TEXT        NOT NULL,
    uploaded_by  BIGINT      REFERENCES "user" (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL        DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL        DEFAULT NOW()
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_document_uploaded_by ON "document" (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_document_created_at ON "document" (created_at);

-- Trigger for document
DROP TRIGGER IF EXISTS trg_document_set_updated_at ON "document";
CREATE TRIGGER trg_document_set_updated_at
    BEFORE UPDATE
    ON "document"
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ========== PROTOCOLS ==========
CREATE TABLE IF NOT EXISTS "protocol"
(
    id          BIGSERIAL PRIMARY KEY,
    public_id   UUID        NOT NULL UNIQUE DEFAULT uuid_generate_v4(),

    code        TEXT        NOT NULL UNIQUE,
    title       TEXT        NOT NULL,
    description TEXT,

    status      TEXT        NOT NULL        DEFAULT 'NEW'
        CHECK (status IN ('NEW', 'PREPARE_FOR_SHIPMENT', 'CANCELED')),

    created_by  BIGINT      REFERENCES "user" (id) ON DELETE SET NULL,
    updated_by  BIGINT      REFERENCES "user" (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL        DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL        DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_protocol_status ON "protocol" (status);
CREATE INDEX IF NOT EXISTS idx_protocol_created_at ON "protocol" (created_at);


-- Trigger for protocol
DROP TRIGGER IF EXISTS trg_protocol_set_updated_at ON "protocol";
CREATE TRIGGER trg_protocol_set_updated_at
    BEFORE UPDATE
    ON "protocol"
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ========== JOIN: PROTOCOL <-> DOCUMENT ==========
CREATE TABLE IF NOT EXISTS "protocol_document"
(
    protocol_id BIGINT NOT NULL REFERENCES "protocol" (id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES "document" (id) ON DELETE CASCADE,
    PRIMARY KEY (protocol_id, document_id)
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_protocol_document_doc ON "protocol_document" (document_id);
