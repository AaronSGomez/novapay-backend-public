-- Migración V2: Tabla de clientes de API para autenticación JWT
-- Los clientes son entidades que acceden al backend (empresas, integradores)

CREATE TABLE IF NOT EXISTS api_clients (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    client_id   VARCHAR(100) NOT NULL UNIQUE,
    hashed_secret VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS api_client_roles (
    client_id VARCHAR(100) NOT NULL REFERENCES api_clients(client_id) ON DELETE CASCADE,
    role      VARCHAR(100) NOT NULL,
    PRIMARY KEY (client_id, role)
);

-- Cliente de prueba inicial
-- clientSecret: 'novapay-secret-2024' (BCrypt $2a$12$...)
INSERT INTO api_clients (id, client_id, hashed_secret, active)
VALUES (
    gen_random_uuid()::text,
    'novapay-client',
    '$2a$12$xWzgNP3y.M.bZJ7v7L3YIeAvp/GMz7lVj7AJpPO/0FtLmZ.6Ul1ra',
    TRUE
) ON CONFLICT (client_id) DO NOTHING;

INSERT INTO api_client_roles (client_id, role)
VALUES ('novapay-client', 'ROLE_INVOICER'),
       ('novapay-client', 'ROLE_FISCAL')
ON CONFLICT DO NOTHING;
