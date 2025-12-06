ALTER TABLE admin_otp
ADD COLUMN used_at TIMESTAMP WITH TIME ZONE;

-- Update the updated_at timestamp on row modification
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
   RETURN NEW;
END;
$$ language 'plpgsql';

   CREATE TRIGGER trg_update_admin_otp_updated_at
BEFORE UPDATE ON admin_otp
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();
-- Create index on used_at for performance
CREATE INDEX idx_admin_otp_used_at ON admin_otp(used_at);