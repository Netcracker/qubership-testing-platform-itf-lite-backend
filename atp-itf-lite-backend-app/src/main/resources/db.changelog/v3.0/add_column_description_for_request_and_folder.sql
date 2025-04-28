ALTER TABLE folders
    ADD COLUMN IF NOT EXISTS description text;

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS description text;
