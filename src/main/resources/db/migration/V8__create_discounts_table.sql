-- Scheduled discounts (issue #15).
--
-- Administrators define discounts with a validity window. A discount
-- is applicable to a checkout only when:
--   active = true AND start_at <= now AND now <= end_at
--
-- NOTE: this migration assumes V7 (sales indexes) is applied first;
-- merge the performance PR before this one.

CREATE TABLE discounts (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    value      NUMERIC(12,2) NOT NULL,
    start_at   TIMESTAMP    NOT NULL,
    end_at     TIMESTAMP    NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_discount_value_non_negative CHECK (value >= 0),
    CONSTRAINT chk_discount_schedule_valid CHECK (end_at >= start_at)
);

CREATE INDEX idx_discounts_active ON discounts(active);
