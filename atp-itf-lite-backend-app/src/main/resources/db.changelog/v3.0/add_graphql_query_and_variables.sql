ALTER TABLE http_requests
    ADD COLUMN IF NOT EXISTS query text,
    ADD COLUMN IF NOT EXISTS variables text;
ALTER TABLE diameter_requests
    ADD COLUMN IF NOT EXISTS query text,
    ADD COLUMN IF NOT EXISTS variables text;