-- GST support (issue #21).
--
-- MRP is tax-inclusive: products.selling_price already includes GST, so
-- these columns describe how an existing amount splits into taxable
-- value and tax. No price or total changes.

-- How each product is taxed.
ALTER TABLE products
    ADD COLUMN tax_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN hsn_code VARCHAR(8);

ALTER TABLE products
    ADD CONSTRAINT chk_products_tax_rate
    CHECK (tax_rate >= 0 AND tax_rate <= 100);

-- Invoice-level tax totals. Nullable on purpose: rows created before
-- GST support genuinely had no tax recorded, and should render without
-- a tax block rather than a misleading 0.00.
ALTER TABLE sales
    ADD COLUMN taxable_amount NUMERIC(12,2),
    ADD COLUMN tax_amount     NUMERIC(12,2),
    ADD COLUMN cgst_amount    NUMERIC(12,2),
    ADD COLUMN sgst_amount    NUMERIC(12,2);

-- Per-line snapshot, so an issued invoice never changes when a product's
-- rate or HSN is edited later.
ALTER TABLE sale_items
    ADD COLUMN tax_rate       NUMERIC(5,2),
    ADD COLUMN hsn_code       VARCHAR(8),
    ADD COLUMN taxable_amount NUMERIC(12,2),
    ADD COLUMN tax_amount     NUMERIC(12,2),
    ADD COLUMN cgst_amount    NUMERIC(12,2),
    ADD COLUMN sgst_amount    NUMERIC(12,2);

-- Store identity, edited from the admin Settings page. Invoice mode is
-- inferred from gstin: empty means "Invoice", present means "Tax Invoice".
CREATE TABLE store_settings (
    id         BIGSERIAL PRIMARY KEY,
    shop_name  VARCHAR(255) NOT NULL,
    address    TEXT,
    phone      VARCHAR(20),
    state_code VARCHAR(2),
    gstin      VARCHAR(15),
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO store_settings (shop_name) VALUES ('RetailFlow');
