-- Run once before deploying the Walk-In invoice tax-breakdown setting.
ALTER TABLE user_settings_entity
    ADD COLUMN disable_tax_breakdown_for_walk_in BOOLEAN DEFAULT FALSE;

UPDATE user_settings_entity
SET disable_tax_breakdown_for_walk_in = FALSE
WHERE disable_tax_breakdown_for_walk_in IS NULL;
