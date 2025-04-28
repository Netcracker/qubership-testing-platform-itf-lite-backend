CREATE TABLE IF NOT EXISTS basic_request_authorizations
(
    username VARCHAR NOT NULL,
    password VARCHAR NOT NULL,
    id uuid NOT NULL
    CONSTRAINT basic_request_authorizations_pkey
        PRIMARY KEY
    CONSTRAINT fk_request_authorizations
        REFERENCES request_authorizations
);