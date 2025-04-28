DROP TABLE IF EXISTS oauth1_request_authorizations;
CREATE TABLE IF NOT EXISTS oauth1_request_authorizations
(
    url VARCHAR(255),
    http_method VARCHAR(255),
    add_data_type VARCHAR(255) NOT NULL,
    signature_method VARCHAR(255) NOT NULL,
    consumer_key VARCHAR(255) NOT NULL,
    consumer_secret VARCHAR(255) NOT NULL,
    access_token VARCHAR(255),
    token_secret VARCHAR(255),
    id uuid NOT NULL
        CONSTRAINT oauth1_request_authorizations_pkey
            PRIMARY KEY
        CONSTRAINT fk1s7j5j5j6j5j1i1iumngwin
            REFERENCES request_authorizations
);