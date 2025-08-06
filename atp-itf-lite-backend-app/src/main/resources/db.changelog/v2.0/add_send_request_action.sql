INSERT INTO actions(id, name, description, deprecated)
SELECT uuid_generate_v1(), 'Execute request "requestPath"', 'Execute request in Itf Lite using provided path', false
WHERE NOT EXISTS (
    SELECT 1 FROM actions WHERE name = 'Execute request "requestPath"'
);
