-- GST-compliant invoice numbering.
--
-- Replaces INV-<uuid>, which was 40 characters of random hex: twice the
-- 16-character limit in Rule 46(b) of the CGST Rules, unreadable over the
-- phone, and not a consecutive series at all.
--
-- New format: INV-26-27-0001 (document type, financial year, serial).
-- The application resets the serial to 1 on April 1, so this table never
-- needs another migration when the year rolls over.
--
-- Existing invoices keep their old numbers: renumbering an issued invoice
-- would break every receipt and reprint that refers to it.
CREATE TABLE invoice_counter (
    id             BIGINT PRIMARY KEY,
    financial_year VARCHAR(7) NOT NULL,
    last_number    BIGINT     NOT NULL DEFAULT 0,

    -- One row only. A second counter row would hand out duplicate
    -- serials, so the database refuses it outright.
    CONSTRAINT chk_invoice_counter_single_row CHECK (id = 1),
    CONSTRAINT chk_invoice_counter_non_negative CHECK (last_number >= 0)
);

-- Seed the current financial year (April 1 to March 31) so the first
-- checkout after this migration only has to bump the row.
INSERT INTO invoice_counter (id, financial_year, last_number)
VALUES (
    1,
    CASE
        WHEN EXTRACT(MONTH FROM CURRENT_DATE) >= 4
            THEN to_char(CURRENT_DATE, 'YY')
                 || '-'
                 || to_char(CURRENT_DATE + INTERVAL '1 year', 'YY')
        ELSE to_char(CURRENT_DATE - INTERVAL '1 year', 'YY')
                 || '-'
                 || to_char(CURRENT_DATE, 'YY')
    END,
    0
);
