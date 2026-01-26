-- V42: Create coupon_usage table
-- Tracks individual coupon redemptions with payment details

CREATE TABLE coupon_usage (
    id VARCHAR(255) PRIMARY KEY,
    coupon_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    original_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2) NOT NULL,
    payment_reference VARCHAR(255),
    subscription_plan VARCHAR(100),
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_usage_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_coupon_usage_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_coupon_usage_coupon_id ON coupon_usage(coupon_id);
CREATE INDEX idx_coupon_usage_user_id ON coupon_usage(user_id);
CREATE INDEX idx_coupon_usage_payment_ref ON coupon_usage(payment_reference);
CREATE INDEX idx_coupon_usage_used_at ON coupon_usage(used_at);
CREATE INDEX idx_coupon_usage_coupon_user ON coupon_usage(coupon_id, user_id);
