ALTER TABLE IF EXISTS request_headers
    ADD COLUMN IF NOT EXISTS generated boolean DEFAULT (false);