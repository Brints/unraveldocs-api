-- V27: Add retry tracking fields to stripe_webhook_events table

ALTER TABLE stripe_webhook_events
ADD COLUMN retry_count INTEGER DEFAULT 0,
ADD COLUMN max_retries_reached BOOLEAN DEFAULT FALSE,
ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE;

-- Index for retry queries
CREATE INDEX idx_stripe_webhook_retry ON stripe_webhook_events(processed, max_retries_reached, next_retry_at)
WHERE processed = FALSE AND max_retries_reached = FALSE;
