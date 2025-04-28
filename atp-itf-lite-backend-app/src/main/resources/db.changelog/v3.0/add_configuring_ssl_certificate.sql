ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS disable_ssl_certificate_verification boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS disable_ssl_client_certificate boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS disable_following_redirect boolean DEFAULT false;
ALTER TABLE folders
    ADD COLUMN IF NOT EXISTS disable_ssl_certificate_verification boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS disable_ssl_client_certificate boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS disable_following_redirect boolean DEFAULT false;
