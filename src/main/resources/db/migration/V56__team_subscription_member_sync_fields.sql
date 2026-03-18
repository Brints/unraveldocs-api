-- Track original and team-derived user subscription context
ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS previous_plan_id VARCHAR(36);

ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS subscription_source VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_user_subscriptions_previous_plan'
    ) THEN
        ALTER TABLE user_subscriptions
            ADD CONSTRAINT fk_user_subscriptions_previous_plan
            FOREIGN KEY (previous_plan_id) REFERENCES subscription_plans(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_previous_plan_id
    ON user_subscriptions (previous_plan_id);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_subscription_source
    ON user_subscriptions (subscription_source);

-- Track when a team entered PAST_DUE for grace-period expiry
ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS past_due_since TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_teams_past_due_since
    ON teams (past_due_since);

