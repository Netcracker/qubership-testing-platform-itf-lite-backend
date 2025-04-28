ALTER TABLE IF EXISTS diameter_requests
    ALTER COLUMN capabilities_exchange_request TYPE TEXT,
    ALTER COLUMN watchdog_default_template TYPE TEXT;

DO '
BEGIN
    IF EXISTS(SELECT *
              FROM information_schema.columns
              WHERE table_name=''diameter_requests'' and column_name=''body'')
    THEN
        ALTER TABLE IF EXISTS diameter_requests
            ALTER COLUMN body TYPE TEXT;
        ALTER TABLE IF EXISTS diameter_requests
            RENAME COLUMN body TO content;
    END IF;
END ';

ALTER TABLE IF EXISTS diameter_requests
    ADD COLUMN IF NOT EXISTS type varchar(255);
