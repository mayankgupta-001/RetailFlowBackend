-- Rename discounts.value -> discounts.discount_value.
--
-- VALUE is a reserved word in H2 (used by the test profile for
-- Hibernate schema generation), so the original column name made the
-- discounts table fail to build in tests. Postgres accepts it, but an
-- explicit name keeps the entity portable across both databases.
--
-- Postgres automatically rewrites the CHECK constraint
-- (chk_discount_value_non_negative) when the referenced column is
-- renamed, so no constraint change is needed here.

ALTER TABLE discounts RENAME COLUMN value TO discount_value;
