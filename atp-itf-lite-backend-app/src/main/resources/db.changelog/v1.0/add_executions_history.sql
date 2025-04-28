-- Postgres
CREATE TABLE IF NOT EXISTS request_executions
(
    id uuid NOT NULL,
    url text NOT NULL,
    name VARCHAR(255) NOT NULL,
    project_id uuid NOT NULL,
    transport_type VARCHAR(50) NOT NULL,
    executed_when TIMESTAMP NOT NULL,
    executor VARCHAR(100) NOT NULL,
    status_code VARCHAR(10),
    status_text VARCHAR(255),
    duration bigint,
    CONSTRAINT request_executions_pkey PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS http_request_execution_details
(
    id uuid NOT NULL,
    request_body text,
    request_headers text,
    response_body text,
    response_headers text,
    error_message text,
    request_execution_id uuid NOT NULL,
    CONSTRAINT http_request_execution_details_pkey PRIMARY KEY (id),
    CONSTRAINT fk_request_executions FOREIGN KEY (request_execution_id) REFERENCES request_executions(id)
    );

CREATE TABLE IF NOT EXISTS diameter_request_execution_details
(
    id uuid NOT NULL,
    capabilities_exchange_request text,
    watchdog_default_template text,
    properties text,
    request_body text,
    response_body text,
    error_message text,
    request_execution_id uuid NOT NULL,
    CONSTRAINT diameter_request_execution_details_pkey PRIMARY KEY (id),
    CONSTRAINT fk_request_executions FOREIGN KEY (request_execution_id) REFERENCES request_executions(id)
    );

CREATE TABLE IF NOT EXISTS user_settings
(
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    visible_columns text,
    created_when  timestamp default CURRENT_TIMESTAMP,
    modified_when timestamp default CURRENT_TIMESTAMP,
    CONSTRAINT user_settings_pkey PRIMARY KEY (id)
    );