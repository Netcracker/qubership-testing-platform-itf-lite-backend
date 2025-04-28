ALTER TABLE request_execution_details
    ADD COLUMN IF NOT EXISTS context_variables text,
    ADD COLUMN IF NOT EXISTS cookies text,
    ADD COLUMN IF NOT EXISTS cookie_header text;
