CREATE TABLE IF NOT EXISTS form_data_part(
    id UUID NOT NULL,
    key varchar(255),
    type varchar(255),
    value TEXT,
    file_id UUID,
    file_size bigint,
    content_type varchar(255),
    description text,
    disabled boolean,
    request_id uuid,
    CONSTRAINT fk_http_requests
        FOREIGN KEY (request_id)
            REFERENCES http_requests(id)
);