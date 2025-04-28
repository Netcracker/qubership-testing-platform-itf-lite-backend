ALTER TABLE request_execution_details
    ADD COLUMN IF NOT EXISTS content text,
    ADD COLUMN IF NOT EXISTS type text,
    ADD COLUMN IF NOT EXISTS form_data_body text,
    ADD COLUMN IF NOT EXISTS file_name text,
    ADD COLUMN IF NOT EXISTS file_id text;

UPDATE request_execution_details a
SET content=(select request_body from request_execution_details b where a.id = b.id), type='JSON'
where request_body IS NOT null and request_body != '';

UPDATE request_execution_details a
SET request_body = null
where request_body IS NOT null and request_body != '';

