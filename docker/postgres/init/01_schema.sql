-- ===== SCHEMA FOR USER/ROLE LOGIC =====

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ===== FUNCTION TO AUTO-UPDATE updated_at =====
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ===== USERS =====
CREATE TABLE IF NOT EXISTS "user"
(
    id            BIGSERIAL PRIMARY KEY,
    public_id     UUID        NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    username      TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL        DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL        DEFAULT NOW(),
    last_login    TIMESTAMPTZ NOT NULL        DEFAULT NOW()
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_user_username ON "user" (username);

-- Trigger update for USERS
DROP TRIGGER IF EXISTS trg_user_set_updated_at ON "user";
CREATE TRIGGER trg_user_set_updated_at
    BEFORE UPDATE
    ON "user"
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- ===== ROLES =====
CREATE TABLE IF NOT EXISTS "role"
(
    id          BIGSERIAL PRIMARY KEY,
    code_name   TEXT        NOT NULL UNIQUE,
    name        TEXT        NOT NULL,
    description TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger for ROLES
DROP TRIGGER IF EXISTS trg_role_set_updated_at ON "role";
CREATE TRIGGER trg_role_set_updated_at
    BEFORE UPDATE
    ON "role"
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ===== AUTHORITIES =====
CREATE TABLE IF NOT EXISTS "authority"
(
    id          BIGSERIAL PRIMARY KEY,
    code_name   TEXT NOT NULL UNIQUE,
    description TEXT
);

-- ===== JOIN: ROLE <-> AUTHORITY =====
CREATE TABLE IF NOT EXISTS "role_authority"
(
    role_id      BIGINT NOT NULL REFERENCES "role" (id) ON DELETE CASCADE,
    authority_id BIGINT NOT NULL REFERENCES "authority" (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, authority_id)
);

-- ===== JOIN: USER <-> ROLE =====
CREATE TABLE IF NOT EXISTS "user_role"
(
    user_id BIGINT NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES "role" (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

