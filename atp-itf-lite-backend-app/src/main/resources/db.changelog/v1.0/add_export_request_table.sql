CREATE TABLE IF NOT EXISTS request_export (
    id UUID NOT NULL,
    request_export_id UUID NOT NULL,
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    request_statuses TEXT NOT NULL,
    destination VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    created_when TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_when TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_request_export PRIMARY KEY (id)
);
