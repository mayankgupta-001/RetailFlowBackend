CREATE TABLE sales (
                       id              BIGSERIAL PRIMARY KEY,
                       invoice_number  VARCHAR(255) NOT NULL UNIQUE,
                       subtotal        NUMERIC(12,2) NOT NULL,
                       discount        NUMERIC(12,2) NOT NULL DEFAULT 0,
                       total           NUMERIC(12,2) NOT NULL,
                       payment_method  VARCHAR(50) NOT NULL,
                       created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_sale_subtotal_non_negative CHECK (subtotal >= 0),
                       CONSTRAINT chk_sale_discount_non_negative CHECK (discount >= 0),
                       CONSTRAINT chk_sale_total_non_negative CHECK (total >= 0)
);