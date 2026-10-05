-- Migración V4: Agregar campos para migración de firma fiscal
-- Permite que clientes existentes importen su hash anterior de firma

-- Entornos históricos pueden no tener la tabla companies aún.
-- Se crea de forma defensiva para permitir aplicar esta migración y las siguientes.
CREATE TABLE IF NOT EXISTS companies (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	name VARCHAR(255) NOT NULL,
	tax_id VARCHAR(255) NOT NULL UNIQUE,
	address VARCHAR(255) NOT NULL,
	tax_agency VARCHAR(50) NOT NULL
);

ALTER TABLE api_clients
ADD COLUMN IF NOT EXISTS client_signing_previous_hash VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS linked_company_id UUID NULL;

-- Agregar constraint de clave foránea
DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_constraint
		WHERE conname = 'fk_api_clients_company'
	) THEN
		ALTER TABLE api_clients
		ADD CONSTRAINT fk_api_clients_company
		FOREIGN KEY (linked_company_id) REFERENCES companies(id);
	END IF;
END $$;

-- Índice para búsquedas
CREATE INDEX IF NOT EXISTS idx_api_clients_company ON api_clients(linked_company_id);

-- Comentarios
COMMENT ON COLUMN api_clients.client_signing_previous_hash 
IS 'Hash SHA-256 anterior del cliente para mantener cadena de firma durante migraciones';
COMMENT ON COLUMN api_clients.linked_company_id 
IS 'Vinculación opcional 1:1 del cliente con una Company específica para emisión fiscal';
