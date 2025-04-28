ALTER TABLE request_executions
ADD COLUMN IF NOT EXISTS testing_status VARCHAR(50);

CREATE TABLE IF NOT EXISTS request_execution_details(
    id UUID NOT NULL,
    request_execution_id uuid,
    request_body TEXT,
    response_body TEXT,
    error_message TEXT,
    request_pre_script TEXT,
    request_post_script TEXT,
    CONSTRAINT request_execution_details_pkey PRIMARY KEY (id),
    CONSTRAINT fk_request_executions FOREIGN KEY (request_execution_id) REFERENCES request_executions(id)
);

DO '
BEGIN
    IF EXISTS(SELECT *
              FROM information_schema.columns
              WHERE table_name=''http_request_execution_details'' AND column_name=''request_execution_id'')
    THEN
        INSERT INTO request_execution_details (id, request_execution_id, request_body, response_body, error_message)
        SELECT id, request_execution_id, request_body, response_body, error_message
        FROM http_request_execution_details;

        ALTER TABLE http_request_execution_details
        ADD CONSTRAINT FK_HTTP_REQUESTS_EXECUTION_DETAILS_ON_ID FOREIGN KEY (id) REFERENCES request_execution_details (id),
        DROP COLUMN IF EXISTS request_execution_id,
        DROP COLUMN IF EXISTS request_body,
        DROP COLUMN IF EXISTS response_body,
        DROP COLUMN IF EXISTS error_message;
    END IF;

    IF EXISTS(SELECT *
              FROM information_schema.columns
              WHERE table_name=''diameter_request_execution_details'' AND column_name=''request_execution_id'')
    THEN
        INSERT INTO request_execution_details (id, request_execution_id, request_body, response_body, error_message)
        SELECT id, request_execution_id, request_body, response_body, error_message
        FROM diameter_request_execution_details;

        ALTER TABLE diameter_request_execution_details
        ADD CONSTRAINT FK_DIAMETER_REQUESTS_EXECUTION_DETAILS_ON_ID FOREIGN KEY (id) REFERENCES request_execution_details (id),
        DROP COLUMN IF EXISTS request_execution_id,
        DROP COLUMN IF EXISTS request_body,
        DROP COLUMN IF EXISTS response_body,
        DROP COLUMN IF EXISTS error_message;
    END IF;
END ';

CREATE TABLE IF NOT EXISTS testing_statuses
(
    id UUID NOT NULL,
    request_execution_details_id UUID,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    CONSTRAINT testing_statuses_pkey PRIMARY KEY (id),
    CONSTRAINT fk_request_execution_details
        FOREIGN KEY (request_execution_details_id)
            REFERENCES request_execution_details(id)
);