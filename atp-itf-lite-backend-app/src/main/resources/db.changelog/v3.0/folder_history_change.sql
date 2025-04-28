ALTER TABLE folders
    ADD COLUMN IF NOT EXISTS permission_info TEXT,
    ADD COLUMN IF NOT EXISTS child_folders TEXT,
    ADD COLUMN IF NOT EXISTS child_requests TEXT;