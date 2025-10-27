-- ===== AUTHORITIES =====
INSERT INTO authority (code_name, description)
VALUES ('CREATE_DOCUMENT', 'Create a document'),
       ('EDIT_DOCUMENT', 'Edit a document'),
       ('VIEW_DOCUMENT', 'View a document'),
       ('CREATE_PROTOCOL', 'Create a protocol'),
       ('EDIT_PROTOCOL', 'Edit a protocol'),
       ('VIEW_PROTOCOL', 'View a protocol')
ON CONFLICT (code_name) DO NOTHING;

-- ===== ROLES =====
INSERT INTO role (code_name, name, description)
VALUES ('DOC_READ', 'Document – Read', 'Can view documents'),
       ('DOC_CREATE', 'Document – Create', 'Can view and create documents'),
       ('DOC_EDIT', 'Document – Edit', 'Can view, create and edit documents'),
       ('PROT_READ', 'Protocol – Read', 'Can view protocols'),
       ('PROT_CREATE', 'Protocol – Create', 'Can view and create protocols'),
       ('PROT_EDIT', 'Protocol – Edit', 'Can view, create and edit protocols')
ON CONFLICT (code_name) DO NOTHING;

-- ===== ROLE ↔ AUTHORITY MAPPINGS =====

-- Documents
INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r
         JOIN authority a ON a.code_name IN ('VIEW_DOCUMENT')
WHERE r.code_name = 'DOC_READ'
ON CONFLICT DO NOTHING;

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r
         JOIN authority a ON a.code_name IN ('VIEW_DOCUMENT', 'CREATE_DOCUMENT')
WHERE r.code_name = 'DOC_CREATE'
ON CONFLICT DO NOTHING;

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r
         JOIN authority a ON a.code_name IN ('VIEW_DOCUMENT', 'CREATE_DOCUMENT', 'EDIT_DOCUMENT')
WHERE r.code_name = 'DOC_EDIT'
ON CONFLICT DO NOTHING;

-- Protocols
INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r
         JOIN authority a ON a.code_name IN ('VIEW_PROTOCOL')
WHERE r.code_name = 'PROT_READ'
ON CONFLICT DO NOTHING;

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r
         JOIN authority a ON a.code_name IN ('VIEW_PROTOCOL', 'CREATE_PROTOCOL')
WHERE r.code_name = 'PROT_CREATE'
ON CONFLICT DO NOTHING;

INSERT INTO role_authority (role_id, authority_id)
SELECT r.id, a.id
FROM role r
         JOIN authority a ON a.code_name IN ('VIEW_PROTOCOL', 'CREATE_PROTOCOL', 'EDIT_PROTOCOL')
WHERE r.code_name = 'PROT_EDIT'
ON CONFLICT DO NOTHING;

-- ===== USERS =====
-- bcrypt hash for password "password"
INSERT INTO "user" (username, password_hash)
VALUES ('reader_doc', '$2a$10$Q9i9f2dXz4bqgK2qk3qV/Ow3m0uY8zY9xC7Qe8w2j2p8zN2rI1Hq2'),
       ('creator_doc', '$2a$10$Q9i9f2dXz4bqgK2qk3qV/Ow3m0uY8zY9xC7Qe8w2j2p8zN2rI1Hq2'),
       ('editor_doc', '$2a$10$Q9i9f2dXz4bqgK2qk3qV/Ow3m0uY8zY9xC7Qe8w2j2p8zN2rI1Hq2'),
       ('protocol_master', '$2a$10$Q9i9f2dXz4bqgK2qk3qV/Ow3m0uY8zY9xC7Qe8w2j2p8zN2rI1Hq2')
ON CONFLICT (username) DO NOTHING;

-- ===== USER ↔ ROLE ASSIGNMENTS =====
WITH pairs(username, role_code) AS (VALUES ('reader_doc', 'DOC_READ'),
                                           ('creator_doc', 'DOC_CREATE'),
                                           ('editor_doc', 'DOC_EDIT'),
                                           ('protocol_master', 'PROT_EDIT'))
INSERT
INTO "user_role" (user_id, role_id)
SELECT u.id, r.id
FROM pairs p
         JOIN "user" u ON u.username = p.username
         JOIN "role" r ON r.code_name = p.role_code
ON CONFLICT DO NOTHING;

