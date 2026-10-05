-- Migración V7: Crear tablas core de facturación/fiscal faltantes para entornos con ddl-auto=validate

CREATE TABLE IF NOT EXISTS pos_terminals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL,
    company_id UUID NOT NULL,
    CONSTRAINT fk_pos_terminals_company
        FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series VARCHAR(255) NOT NULL,
    number VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    company_id UUID NOT NULL,
    terminal_id UUID NOT NULL,
    issue_date DATE NOT NULL,
    base_amount NUMERIC(19,2),
    tax_amount NUMERIC(19,2),
    total_amount NUMERIC(19,2),
    qr_code VARCHAR(255),
    rectified_invoice_id UUID NULL,
    CONSTRAINT fk_invoices_company
        FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_invoices_terminal
        FOREIGN KEY (terminal_id) REFERENCES pos_terminals(id),
    CONSTRAINT fk_invoices_rectified
        FOREIGN KEY (rectified_invoice_id) REFERENCES invoices(id)
);

CREATE TABLE IF NOT EXISTS invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    tax_type VARCHAR(50) NOT NULL,
    base_amount NUMERIC(19,2),
    tax_amount NUMERIC(19,2),
    total_amount NUMERIC(19,2),
    CONSTRAINT fk_invoice_lines_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

CREATE TABLE IF NOT EXISTS fiscal_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL UNIQUE,
    agency VARCHAR(50) NOT NULL,
    previous_hash VARCHAR(255),
    current_hash VARCHAR(255),
    sent_at TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    sent_xml TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    response_xml TEXT,
    CONSTRAINT fk_fiscal_records_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

CREATE INDEX IF NOT EXISTS idx_pos_terminals_company_id ON pos_terminals(company_id);
CREATE INDEX IF NOT EXISTS idx_invoices_company_id ON invoices(company_id);
CREATE INDEX IF NOT EXISTS idx_invoices_terminal_id ON invoices(terminal_id);
CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);
