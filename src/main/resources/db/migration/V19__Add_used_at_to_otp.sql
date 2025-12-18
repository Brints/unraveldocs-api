-- 1. Use IF NOT EXISTS for the column
ALTER TABLE admin_otp
    ADD COLUMN IF NOT EXISTS used_at TIMESTAMP WITH TIME ZONE;

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 3. Drop the trigger if it exists before creating it to avoid conflicts
DROP TRIGGER IF EXISTS trg_update_admin_otp_updated_at ON admin_otp;

CREATE TRIGGER trg_update_admin_otp_updated_at
    BEFORE UPDATE ON admin_otp
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();

-- 4. Use IF NOT EXISTS for the index
CREATE INDEX IF NOT EXISTS idx_admin_otp_used_at ON admin_otp(used_at);