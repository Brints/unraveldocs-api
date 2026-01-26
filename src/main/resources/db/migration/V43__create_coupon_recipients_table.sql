-- V43: Create coupon_recipients table
-- Tracks specific user assignments for targeted coupons and notification status

CREATE TABLE coupon_recipients (
    id VARCHAR(255) PRIMARY KEY,
    coupon_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    notified_at TIMESTAMP WITH TIME ZONE,
    expiry_notified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_recipients_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_recipients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_coupon_user UNIQUE (coupon_id, user_id)
);

CREATE INDEX idx_coupon_recipients_coupon_id ON coupon_recipients(coupon_id);
CREATE INDEX idx_coupon_recipients_user_id ON coupon_recipients(user_id);
CREATE INDEX idx_coupon_recipients_notified ON coupon_recipients(notified_at);
