-- Add new columns with NULL allowed first
ALTER TABLE users ADD COLUMN terms_accepted BOOLEAN;
ALTER TABLE users ADD COLUMN date_of_birth DATE;
ALTER TABLE users ADD COLUMN profession VARCHAR(100);
ALTER TABLE users ADD COLUMN marketing_opt_in BOOLEAN;
ALTER TABLE users ADD COLUMN country VARCHAR(100);
ALTER TABLE users ADD COLUMN organization VARCHAR(255);

-- Set default values for existing rows
UPDATE users SET terms_accepted = false WHERE terms_accepted IS NULL;
UPDATE users SET marketing_opt_in = false WHERE marketing_opt_in IS NULL;
UPDATE users SET country = 'Unknown' WHERE country IS NULL;

-- Now add NOT NULL constraints
ALTER TABLE users ALTER COLUMN terms_accepted SET NOT NULL;
ALTER TABLE users ALTER COLUMN marketing_opt_in SET NOT NULL;
ALTER TABLE users ALTER COLUMN country SET NOT NULL;

-- Add defaults for new rows
ALTER TABLE users ALTER COLUMN terms_accepted SET DEFAULT false;
ALTER TABLE users ALTER COLUMN marketing_opt_in SET DEFAULT false;
ALTER TABLE users ALTER COLUMN country SET DEFAULT 'Unknown';

-- Add indexes
CREATE INDEX idx_users_country ON users(country);
CREATE INDEX idx_users_date_of_birth ON users(date_of_birth);

-- Add comments
COMMENT ON COLUMN users.terms_accepted IS 'Indicates if user has accepted terms and conditions';
COMMENT ON COLUMN users.date_of_birth IS 'User date of birth for age verification';
COMMENT ON COLUMN users.profession IS 'User profession or occupation';
COMMENT ON COLUMN users.marketing_opt_in IS 'Indicates if user has opted in to marketing communications';
COMMENT ON COLUMN users.country IS 'User country of residence';
COMMENT ON COLUMN users.organization IS 'User organization or industry';
