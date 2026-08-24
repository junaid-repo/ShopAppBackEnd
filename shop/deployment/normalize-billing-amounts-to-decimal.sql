-- Run once before deploying the billing normalization code.
-- Amounts use exact two-decimal storage; percentages retain fractional rates such as 2.5%.

ALTER TABLE billing_details
    MODIFY tax_amount DECIMAL(19,2) NULL,
    MODIFY sub_total_amount DECIMAL(19,2) NULL,
    MODIFY total_amount DECIMAL(19,2) NULL,
    MODIFY paying_amount DECIMAL(19,2) NULL,
    MODIFY remaining_amount DECIMAL(19,2) NULL,
    MODIFY total_profit_oncp DECIMAL(19,2) NULL,
    MODIFY discount_percent DECIMAL(7,4) NULL,
    ADD COLUMN discount_amount DECIMAL(19,2) DEFAULT 0.00,
    ADD COLUMN cgst_amount DECIMAL(19,2) DEFAULT 0.00,
    ADD COLUMN sgst_amount DECIMAL(19,2) DEFAULT 0.00,
    ADD COLUMN igst_amount DECIMAL(19,2) DEFAULT 0.00;

ALTER TABLE billing_payments
    MODIFY tax DECIMAL(19,2) NULL,
    MODIFY subtotal DECIMAL(19,2) NULL,
    MODIFY total DECIMAL(19,2) NULL,
    MODIFY paid DECIMAL(19,2) NULL,
    MODIFY to_be_paid DECIMAL(19,2) NULL;

ALTER TABLE product_sales
    MODIFY tax DECIMAL(19,2) NULL,
    MODIFY sub_total DECIMAL(19,2) NULL,
    MODIFY total DECIMAL(19,2) NULL,
    MODIFY cgst DECIMAL(19,2) NULL,
    MODIFY sgst DECIMAL(19,2) NULL,
    MODIFY igst DECIMAL(19,2) NULL,
    MODIFY profit_oncp DECIMAL(19,2) NULL,
    MODIFY discount_percentage DECIMAL(7,4) NULL,
    MODIFY cgst_percentage DECIMAL(7,4) NULL,
    MODIFY sgst_percentage DECIMAL(7,4) NULL,
    MODIFY igst_percentage DECIMAL(7,4) NULL,
    ADD COLUMN discount_amount DECIMAL(19,2) DEFAULT 0.00;

ALTER TABLE billing_gst
    MODIFY gst_percentage DECIMAL(7,4) NULL,
    MODIFY gst_amount DECIMAL(19,2) NULL;

ALTER TABLE billing_payments_history
    MODIFY paid_amount DECIMAL(19,2) NULL;

ALTER TABLE shop_customer
    MODIFY total_spent DECIMAL(19,2) NULL;
