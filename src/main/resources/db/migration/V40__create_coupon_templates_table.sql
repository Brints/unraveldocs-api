-- V40: Create coupon_templates table
-- This must be created before coupons table due to foreign key constraint

CREATE TABLE coupon_templates (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    discount_percentage DECIMAL(5,2) NOT NULL CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    min_purchase_amount DECIMAL(10,2) DEFAULT NULL,
    recipient_category VARCHAR(50) NOT NULL DEFAULT 'ALL_PAID_USERS',
    max_usage_count INTEGER DEFAULT NULL,
    max_usage_per_user INTEGER DEFAULT 1,
    validity_days INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_templates_created_by ON coupon_templates(created_by);
CREATE INDEX idx_templates_name ON coupon_templates(name);
CREATE INDEX idx_templates_is_active ON coupon_templates(is_active);
