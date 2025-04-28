ALTER TABLE IF EXISTS request_executions
    ADD COLUMN IF NOT EXISTS sse_id UUID,
    ADD COLUMN IF NOT EXISTS authorization_token TEXT;

ALTER TABLE IF EXISTS request_export
    RENAME COLUMN session_id TO sse_id;
