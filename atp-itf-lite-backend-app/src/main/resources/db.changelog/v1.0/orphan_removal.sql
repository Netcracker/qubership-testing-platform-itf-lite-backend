-- Postgres
DELETE FROM request_params WHERE request_id is NULL;

DELETE FROM request_headers WHERE request_id is NULL;