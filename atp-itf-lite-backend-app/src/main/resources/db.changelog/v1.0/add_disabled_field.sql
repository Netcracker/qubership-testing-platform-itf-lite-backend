ALTER TABLE IF EXISTS request_headers ADD COLUMN IF NOT EXISTS disabled boolean DEFAULT(false);
ALTER TABLE IF EXISTS request_params ADD COLUMN IF NOT EXISTS disabled boolean DEFAULT(false);