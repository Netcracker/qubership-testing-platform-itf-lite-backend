ALTER TABLE IF EXISTS folders
    ADD COLUMN IF NOT EXISTS disable_auto_encoding BOOLEAN DEFAULT (false);

ALTER TABLE IF EXISTS requests
    ADD COLUMN IF NOT EXISTS disable_auto_encoding BOOLEAN DEFAULT (false);
