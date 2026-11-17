-- Performance indexes for the sales module (Postgres-only).
--
-- 1. created_at index: the ADMIN report endpoint filters with
--    created_at BETWEEN and every listing orders by created_at DESC.
--    Without this index Postgres does a full table scan + sort.
--
-- 2. Trigram (pg_trgm) GIN index on invoice_number: invoice lookup
--    uses LIKE '%query%' (partial, case-insensitive), which a standard
--    B-tree index cannot serve. The trigram index turns it into an
--    index scan.
--
-- This migration must stay Postgres-only; the "test" profile skips
-- Flyway and builds its H2 schema from the JPA entities instead.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_sales_created_at
    ON sales (created_at);

CREATE INDEX idx_sales_invoice_number_trgm
    ON sales USING GIN (invoice_number gin_trgm_ops);
