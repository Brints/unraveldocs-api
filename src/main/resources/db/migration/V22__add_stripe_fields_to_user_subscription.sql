-- Add Stripe-specific fields to user_subscriptions table
ALTER TABLE user_subscriptions
ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS stripe_payment_method_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS stripe_latest_invoice_id VARCHAR(255);

-- Add foreign key constraint
ALTER TABLE user_subscriptions
ADD CONSTRAINT fk_user_subscription_stripe_customer
FOREIGN KEY (stripe_customer_id) REFERENCES stripe_customers(id) ON DELETE SET NULL;

-- Create index
CREATE INDEX IF NOT EXISTS idx_user_subscription_stripe_customer ON user_subscriptions(stripe_customer_id);
