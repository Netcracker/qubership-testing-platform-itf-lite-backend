ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS preScripts text,
    ADD COLUMN IF NOT EXISTS postScripts text;