-- Add composite index for OTP expiration queries
-- This index optimizes the scheduled cleanup tasks that check both expires_at and is_expired

CREATE INDEX idx_admin_otp_expiry_status ON admin_otp(expires_at, is_expired);

-- This composite index will be used by:
-- 1. The markExpiredOtps scheduled task (every 5 minutes)
-- 2. The deleteExpiredOtpsOlderThan scheduled task (daily at 2 AM)

-- The composite index is more efficient than using the separate indexes
-- when both columns are used in WHERE clauses.

