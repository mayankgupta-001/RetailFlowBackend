CREATE TABLE sale_items (
                            id            BIGSERIAL PRIMARY KEY,
                            sale_id       BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
                            product_id    BIGINT NOT NULL REFERENCES products(id),
                            product_name  VARCHAR(255) NOT NULL,
                            barcode       VARCHAR(255) NOT NULL,
                            price         NUMERIC(12,2) NOT NULL,
                            quantity      INTEGER NOT NULL,
                            subtotal      NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_sale_items_sale_id ON sale_items(sale_id);
CREATE INDEX idx_sale_items_product_id ON sale_items(product_id);