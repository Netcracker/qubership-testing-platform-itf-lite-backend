ALTER TABLE oauth2_request_authorizations
ADD COLUMN IF NOT EXISTS token TEXT;