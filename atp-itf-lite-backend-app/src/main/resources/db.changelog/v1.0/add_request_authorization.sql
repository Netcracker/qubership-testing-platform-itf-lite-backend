CREATE TABLE IF NOT EXISTS request_authorizations
(
    id uuid NOT NULL
        CONSTRAINT request_authorizations_pkey
            PRIMARY KEY,
    type VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS oauth2_request_authorizations
(
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255),
    grant_type VARCHAR(255) NOT NULL,
    header_prefix VARCHAR(255),
    password VARCHAR(255),
    scope VARCHAR(255),
    url VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    id uuid NOT NULL
        CONSTRAINT oauth2_request_authorizations_pkey
            PRIMARY KEY
        CONSTRAINT fkswox7l3p6hut5j1i1iumngwin
            REFERENCES request_authorizations
);

ALTER TABLE requests DROP CONSTRAINT IF EXISTS fkds3mn2jytooxu262rlxqvgi84;

ALTER TABLE IF EXISTS requests
    ADD COLUMN IF NOT EXISTS authorization_id uuid
        constraint fkds3mn2jytooxu262rlxqvgi84
        references request_authorizations;