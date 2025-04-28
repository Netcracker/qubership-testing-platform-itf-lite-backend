CREATE TABLE IF NOT EXISTS request_snapshots (
    session_id UUID,
    request_id UUID,
    created_when TIMESTAMP,
    request TEXT,
    binary_file_id UUID,
    PRIMARY KEY (session_id, request_id)
);
