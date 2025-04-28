CREATE TABLE IF NOT EXISTS bearer_request_authorizations
(
    token VARCHAR NOT NULL,
    id uuid NOT NULL
    CONSTRAINT bearer_request_authorizations_pkey
        PRIMARY KEY
    CONSTRAINT skdamx5k5p6hut1j8i2iudslfs
        REFERENCES request_authorizations
);