CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,

                          name VARCHAR(255) NOT NULL,

                          barcode VARCHAR(100) NOT NULL UNIQUE,

                          selling_price DECIMAL(12,2) NOT NULL,

                          cost_price DECIMAL(12,2),

                          stock INTEGER NOT NULL DEFAULT 0,

                          min_stock INTEGER NOT NULL DEFAULT 0,

                          category VARCHAR(100),

                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT chk_stock_non_negative CHECK (stock >= 0),

                          CONSTRAINT chk_min_stock_non_negative CHECK (min_stock >= 0),

                          CONSTRAINT chk_selling_price_non_negative CHECK (selling_price >= 0),

                          CONSTRAINT chk_cost_price_non_negative CHECK (
                              cost_price IS NULL OR cost_price >= 0
                              )
);