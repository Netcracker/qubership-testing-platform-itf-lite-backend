ALTER TABLE IF EXISTS request_executions
    ADD COLUMN IF NOT EXISTS sse_id UUID,
    ADD COLUMN IF NOT EXISTS authorization_token TEXT;

DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_name = 'request_export'
              AND column_name = 'session_id'
        ) THEN
            ALTER TABLE request_export
                RENAME COLUMN session_id TO sse_id;
        END IF;
    END$$;
