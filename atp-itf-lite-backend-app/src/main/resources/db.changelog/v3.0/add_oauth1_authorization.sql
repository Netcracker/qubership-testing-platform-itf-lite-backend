-- 1. Create a table if it does not already exist (with a primary key and FK)
CREATE TABLE IF NOT EXISTS oauth1_request_authorizations
(
    id uuid NOT NULL
        CONSTRAINT oauth1_request_authorizations_pkey
            PRIMARY KEY
        CONSTRAINT fk1s7j5j5j6j5j1i1iumngwin
            REFERENCES request_authorizations
);

-- 2. Add the missing columns (if any)
ALTER TABLE IF EXISTS oauth1_request_authorizations
    ADD COLUMN IF NOT EXISTS url              VARCHAR(255),
    ADD COLUMN IF NOT EXISTS http_method      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS add_data_type    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signature_method VARCHAR(255),
    ADD COLUMN IF NOT EXISTS consumer_key     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS consumer_secret  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS access_token     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS token_secret     VARCHAR(255);

-- 3. Guarantee NOT NULL and default values (only if the column already exists)
DO
$$
    BEGIN
        -- add_data_type
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'oauth1_request_authorizations'
                     AND column_name = 'add_data_type') THEN
            ALTER TABLE oauth1_request_authorizations
                ALTER COLUMN add_data_type SET NOT NULL;
        END IF;

        -- signature_method
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'oauth1_request_authorizations'
                     AND column_name = 'signature_method') THEN
            ALTER TABLE oauth1_request_authorizations
                ALTER COLUMN signature_method SET NOT NULL;
        END IF;

        -- consumer_key
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'oauth1_request_authorizations'
                     AND column_name = 'consumer_key') THEN
            ALTER TABLE oauth1_request_authorizations
                ALTER COLUMN consumer_key SET NOT NULL;
        END IF;

        -- consumer_secret
        IF EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_name = 'oauth1_request_authorizations'
                     AND column_name = 'consumer_secret') THEN
            ALTER TABLE oauth1_request_authorizations
                ALTER COLUMN consumer_secret SET NOT NULL;
        END IF;
    END
$$
