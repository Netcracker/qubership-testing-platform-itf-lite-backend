ALTER TABLE folders
    ADD COLUMN IF NOT EXISTS permission_folder_id uuid;

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS permission_folder_id uuid;