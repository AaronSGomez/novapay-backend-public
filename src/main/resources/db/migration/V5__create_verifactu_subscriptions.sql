CREATE TABLE IF NOT EXISTS verifactu_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
    plan_code VARCHAR(50) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    included_invoices INTEGER NOT NULL,
    base_amount NUMERIC(10,2) NOT NULL,
    overage_per_invoice NUMERIC(10,2) NOT NULL,
    current_period_invoices INTEGER NOT NULL DEFAULT 0,
    period_start TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verifactu_subscriptions_company_id
    ON verifactu_subscriptions(company_id);
