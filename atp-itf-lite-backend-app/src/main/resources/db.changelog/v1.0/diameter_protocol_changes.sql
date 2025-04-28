CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS diameter_requests (
    id UUID NOT NULL,
    host VARCHAR(255),
    port VARCHAR(255),
    capabilities_exchange_request VARCHAR(255),
    watchdog_default_template VARCHAR(255),
    connection_layer VARCHAR(255),
    response_type VARCHAR(255),
    message_format VARCHAR(255),
    dictionary_type VARCHAR(255),
    response_timeout INTEGER,
    body VARCHAR(255),
    CONSTRAINT pk_diameter_requests PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS http_requests (
    id UUID NOT NULL,
    http_method VARCHAR(255),
    url VARCHAR(255),
    content TEXT,
    type VARCHAR(255),
    CONSTRAINT pk_http_requests PRIMARY KEY (id)
);

ALTER TABLE diameter_requests DROP CONSTRAINT IF EXISTS FK_DIAMETER_REQUESTS_ON_ID;
ALTER TABLE diameter_requests ADD CONSTRAINT FK_DIAMETER_REQUESTS_ON_ID FOREIGN KEY (id) REFERENCES requests (id);

ALTER TABLE http_requests DROP CONSTRAINT IF EXISTS FK_HTTP_REQUESTS_ON_ID;
ALTER TABLE http_requests ADD CONSTRAINT FK_HTTP_REQUESTS_ON_ID FOREIGN KEY (id) REFERENCES requests (id);

ALTER TABLE request_headers DROP CONSTRAINT IF EXISTS FK_REQUEST_HEADERS_ON_REQUEST;
ALTER TABLE request_headers ADD CONSTRAINT FK_REQUEST_HEADERS_ON_REQUEST FOREIGN KEY (request_id) REFERENCES http_requests (id);

ALTER TABLE request_params DROP CONSTRAINT IF EXISTS FK_REQUEST_PARAMS_ON_REQUEST;
ALTER TABLE request_params ADD CONSTRAINT FK_REQUEST_PARAMS_ON_REQUEST FOREIGN KEY (request_id) REFERENCES http_requests (id);

ALTER TABLE request_headers DROP CONSTRAINT IF EXISTS fk644b1yxpctg4o8d01ejicwyyy;

ALTER TABLE request_params DROP CONSTRAINT IF EXISTS fkgospad6h535i7126kel228up6;

ALTER TABLE requests DROP COLUMN IF EXISTS content;
ALTER TABLE requests DROP COLUMN IF EXISTS http_method;
ALTER TABLE requests DROP COLUMN IF EXISTS type;
ALTER TABLE requests DROP COLUMN IF EXISTS url;
