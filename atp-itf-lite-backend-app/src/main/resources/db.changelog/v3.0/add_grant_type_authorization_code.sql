ALTER TABLE oauth2_request_authorizations
    ADD COLUMN IF NOT EXISTS auth_url VARCHAR (255),
    ADD COLUMN IF NOT EXISTS state VARCHAR(255);

CREATE TABLE IF NOT EXISTS get_authorization_code (
    sse_id uuid NOT NULL PRIMARY KEY,
    project_id uuid NOT NULL,
    started_at timestamp without time zone NOT NULL DEFAULT now(),
    access_token_url VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255),
    scope VARCHAR(255),
    state VARCHAR(255),
    redirect_uri VARCHAR(255),
    response_state VARCHAR(255),
    username VARCHAR(255),
    authorization_code VARCHAR(255),
    token VARCHAR(255)
);