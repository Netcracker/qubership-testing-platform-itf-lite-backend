ALTER TABLE requests ADD COLUMN IF NOT EXISTS source_id uuid;
CREATE INDEX IF NOT EXISTS projectIdAndSourceIdOnRequests ON requests (project_id, source_id);