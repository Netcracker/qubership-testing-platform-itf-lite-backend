ALTER TABLE IF EXISTS request_params
    ADD COLUMN IF NOT EXISTS generated boolean DEFAULT (false);