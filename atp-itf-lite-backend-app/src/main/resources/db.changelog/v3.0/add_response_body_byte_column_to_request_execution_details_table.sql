ALTER TABLE request_execution_details
ADD COLUMN IF NOT EXISTS response_body_byte bytea;