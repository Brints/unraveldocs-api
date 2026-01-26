-- V41: Create coupons table
-- Main coupon definitions with recipient targeting and optional template reference

CREATE TABLE coupons (
    id VARCHAR(255) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    is_custom_code BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    recipient_category VARCHAR(50) NOT NULL DEFAULT 'ALL_PAID_USERS',
    discount_percentage DECIMAL(5,2) NOT NULL CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    min_purchase_amount DECIMAL(10,2) DEFAULT NULL,
    max_usage_count INTEGER DEFAULT NULL,
    max_usage_per_user INTEGER DEFAULT 1,
    current_usage_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    template_id VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_coupon_template FOREIGN KEY (template_id) REFERENCES coupon_templates(id)
);

CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_is_active ON coupons(is_active);
CREATE INDEX idx_coupons_valid_dates ON coupons(valid_from, valid_until);
CREATE INDEX idx_coupons_valid_until ON coupons(valid_until);
CREATE INDEX idx_coupons_created_by ON coupons(created_by);
CREATE INDEX idx_coupons_recipient_category ON coupons(recipient_category);
CREATE INDEX idx_coupons_template_id ON coupons(template_id);
CREATE INDEX idx_coupons_expiry_notification ON coupons(expiry_notification_sent, valid_until);
