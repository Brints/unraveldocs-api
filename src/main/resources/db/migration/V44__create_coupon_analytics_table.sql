-- V44: Create coupon_analytics table
-- Stores pre-calculated daily analytics for efficient time-series and cohort analysis

CREATE TABLE coupon_analytics (
    id VARCHAR(255) PRIMARY KEY,
    coupon_id VARCHAR(255) NOT NULL,
    analytics_date DATE NOT NULL,
    usage_count INTEGER DEFAULT 0,
    total_discount_amount DECIMAL(10,2) DEFAULT 0,
    total_original_amount DECIMAL(10,2) DEFAULT 0,
    total_final_amount DECIMAL(10,2) DEFAULT 0,
    unique_users_count INTEGER DEFAULT 0,
    by_subscription_plan JSONB,
    by_recipient_category JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analytics_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT unique_coupon_date UNIQUE(coupon_id, analytics_date)
);

CREATE INDEX idx_analytics_coupon_id ON coupon_analytics(coupon_id);
CREATE INDEX idx_analytics_date ON coupon_analytics(analytics_date);
CREATE INDEX idx_analytics_coupon_date ON coupon_analytics(coupon_id, analytics_date);
