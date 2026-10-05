-- Migración V3: Agregar campos de email y autenticación
-- Transforma api_clients de clientId/clientSecret a email/password
-- Mantiene backward compatibility con clientId

-- Agregar columnas de email
ALTER TABLE api_clients 
ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS email_verification_token_expires TIMESTAMPTZ NULL;

-- Agregar columnas de recuperación de contraseña
ALTER TABLE api_clients 
ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS password_reset_token_expires TIMESTAMPTZ NULL,
ADD COLUMN IF NOT EXISTS reset_password_temporary VARCHAR(255) NULL;

-- Agregar audit fields
ALTER TABLE api_clients 
ADD COLUMN IF NOT EXISTS last_login TIMESTAMPTZ NULL;

-- Crear índices para búsquedas
CREATE UNIQUE INDEX IF NOT EXISTS idx_api_clients_email_uq ON api_clients(email);
CREATE INDEX IF NOT EXISTS idx_api_clients_verification_token ON api_clients(email_verification_token);
CREATE INDEX IF NOT EXISTS idx_api_clients_reset_token ON api_clients(password_reset_token);

-- Comentarios para documentación
COMMENT ON COLUMN api_clients.email IS 'Email único del cliente para autenticación y recuperación';
COMMENT ON COLUMN api_clients.email_verified IS 'Flag booleano indicando si el email ha sido verificado';
COMMENT ON COLUMN api_clients.email_verification_token IS 'Token sha256 para verificar email (hash del token enviado)';
COMMENT ON COLUMN api_clients.email_verification_token_expires IS 'Timestamp de expiración del token de verificación';
COMMENT ON COLUMN api_clients.password_reset_token IS 'Token sha256 para resetear contraseña (hash del token enviado)';
COMMENT ON COLUMN api_clients.password_reset_token_expires IS 'Timestamp de expiración del token de reset';
COMMENT ON COLUMN api_clients.reset_password_temporary IS 'BCrypt hash de la contraseña temporal generada';
COMMENT ON COLUMN api_clients.created_at IS 'Timestamp de creación del cliente (cannot change)';
COMMENT ON COLUMN api_clients.last_login IS 'Timestamp del último login exitoso';
