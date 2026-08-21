CREATE TABLE inventory_logs (

                                id BIGSERIAL PRIMARY KEY,

                                product_id BIGINT NOT NULL,

                                type VARCHAR(30) NOT NULL,

                                quantity INTEGER NOT NULL,

                                previous_stock INTEGER NOT NULL,

                                new_stock INTEGER NOT NULL,

                                reason VARCHAR(255),

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_inventory_product
                                    FOREIGN KEY (product_id)
                                        REFERENCES products(id)
);