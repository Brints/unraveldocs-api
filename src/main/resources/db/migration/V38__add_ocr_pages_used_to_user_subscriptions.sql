-- Add ocr_pages_used column to user_subscriptions table
ALTER TABLE user_subscriptions ADD COLUMN ocr_pages_used INTEGER NOT NULL DEFAULT 0;

-- Add index for faster queries on OCR usage
CREATE INDEX idx_user_subscriptions_ocr_pages_used ON user_subscriptions(ocr_pages_used);
