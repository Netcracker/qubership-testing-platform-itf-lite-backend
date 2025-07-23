CREATE INDEX IF NOT EXISTS idx_request_params_request_id ON request_params(request_id);
CREATE INDEX IF NOT EXISTS idx_request_headers_request_id ON request_headers(request_id);