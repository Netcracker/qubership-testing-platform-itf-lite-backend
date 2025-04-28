CREATE TABLE IF NOT EXISTS inherit_from_parent_request_authorizations
(
    authorization_folder_id UUID,
    id UUID NOT NULL
        CONSTRAINT inherit_auth_pkey
            PRIMARY KEY
        CONSTRAINT fk_request_authorization
            REFERENCES request_authorizations
);

ALTER TABLE folders
ADD COLUMN IF NOT EXISTS authorization_id UUID;
