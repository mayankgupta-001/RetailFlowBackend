CREATE TABLE discounts (
                           id              BIGSERIAL PRIMARY KEY,
                           name            VARCHAR(100) NOT NULL,
                           type            VARCHAR(20) NOT NULL,
                           discount_value  NUMERIC(12, 2) NOT NULL,
                           start_at        TIMESTAMP NOT NULL,
                           end_at          TIMESTAMP NOT NULL,
                           active          BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at      TIMESTAMP NOT NULL,
                           updated_at      TIMESTAMP NOT NULL,

                           CONSTRAINT chk_discounts_type
                               CHECK (type IN ('FIXED', 'PERCENTAGE')),

                           CONSTRAINT chk_discounts_value
                               CHECK (discount_value >= 0),

                           CONSTRAINT chk_discounts_date_range
                               CHECK (start_at <= end_at)
);