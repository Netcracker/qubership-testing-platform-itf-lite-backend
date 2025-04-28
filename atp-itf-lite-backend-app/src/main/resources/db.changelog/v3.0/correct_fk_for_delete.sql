ALTER TABLE IF EXISTS request_execution_details
    DROP CONSTRAINT IF EXISTS fk_request_executions,
    ADD CONSTRAINT fk_request_executions
        FOREIGN KEY (request_execution_id)
            REFERENCES request_executions(id) on delete cascade;

ALTER TABLE IF EXISTS http_request_execution_details
    DROP CONSTRAINT IF EXISTS fk_http_requests_execution_details_on_id,
    ADD CONSTRAINT fk_http_requests_execution_details_on_id
        FOREIGN KEY (id)
            REFERENCES request_execution_details(id) on delete cascade;

ALTER TABLE IF EXISTS diameter_request_execution_details
    DROP CONSTRAINT IF EXISTS fk_diameter_requests_execution_details_on_id,
    ADD CONSTRAINT fk_diameter_requests_execution_details_on_id
        FOREIGN KEY (id)
            REFERENCES request_execution_details(id) on delete cascade;

ALTER TABLE IF EXISTS testing_statuses
    DROP CONSTRAINT IF EXISTS fk_request_execution_details,
    ADD CONSTRAINT fk_request_execution_details
        FOREIGN KEY (request_execution_details_id)
            REFERENCES request_execution_details(id) on delete cascade;
