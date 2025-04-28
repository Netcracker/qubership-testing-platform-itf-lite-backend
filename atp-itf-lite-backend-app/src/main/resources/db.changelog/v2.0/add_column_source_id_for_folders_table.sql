ALTER TABLE folders ADD COLUMN IF NOT EXISTS source_id uuid;
CREATE INDEX IF NOT EXISTS projectIdAndSourceIdOnFolders ON folders (project_id, source_id);