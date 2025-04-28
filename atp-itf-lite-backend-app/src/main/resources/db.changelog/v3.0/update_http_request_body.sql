ALTER TABLE http_requests
    ADD COLUMN IF NOT EXISTS file_name text,
    ADD COLUMN IF NOT EXISTS file_id text;
ALTER TABLE diameter_requests
    ADD COLUMN IF NOT EXISTS file_name text,
    ADD COLUMN IF NOT EXISTS file_id text,
    ADD COLUMN IF NOT EXISTS dictionary_id text;
