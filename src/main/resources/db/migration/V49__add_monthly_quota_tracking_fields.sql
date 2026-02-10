-- Add monthly quota tracking fields to user_subscriptions
-- These fields track usage within the current billing period and reset monthly

-- Add monthly_documents_uploaded to track documents uploaded in current billing period
ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS monthly_documents_uploaded INTEGER NOT NULL DEFAULT 0;

-- Add quota_reset_date to track when the monthly quota should be reset
-- This is set to the first day of the next month
ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS quota_reset_date TIMESTAMP WITH TIME ZONE;

-- Initialize quota_reset_date for existing subscriptions
-- Set it to the first day of the next month from now
UPDATE user_subscriptions
SET quota_reset_date = DATE_TRUNC('month', CURRENT_TIMESTAMP) + INTERVAL '1 month'
WHERE quota_reset_date IS NULL;

-- Reset ocr_pages_used for existing subscriptions since we're now treating it as monthly
-- Note: This is optional - comment out if you want to preserve existing OCR usage counts
-- UPDATE user_subscriptions SET ocr_pages_used = 0;

-- Create index for efficient quota reset job queries
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_quota_reset_date
    ON user_subscriptions(quota_reset_date);

-- Add comment explaining the monthly quota logic
COMMENT ON COLUMN user_subscriptions.monthly_documents_uploaded IS 'Number of documents uploaded in the current billing period. Resets monthly.';
COMMENT ON COLUMN user_subscriptions.quota_reset_date IS 'Date when monthly quotas (documents, OCR pages) should be reset. Set to first day of next month.';
COMMENT ON COLUMN user_subscriptions.ocr_pages_used IS 'Number of OCR pages used in the current billing period. Resets monthly.';
