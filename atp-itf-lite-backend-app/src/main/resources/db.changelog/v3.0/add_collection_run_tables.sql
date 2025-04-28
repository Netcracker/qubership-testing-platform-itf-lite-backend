CREATE TABLE IF NOT EXISTS collection_run_next_request
(
    id                UUID NOT NULL,
    collection_run_id UUID,
    next_request      VARCHAR(255),
    created_when      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_collection_run_next_request PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collection_run_requests_count
(
    id                UUID NOT NULL,
    collection_run_id UUID,
    request_id        UUID,
    request_name      VARCHAR(255),
    count             INTEGER,
    created_when      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_collection_run_requests_count PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collection_run_requests_order
(
    id                UUID NOT NULL,
    collection_run_id UUID,
    request_id        UUID,
    request_name      VARCHAR(255),
    request_order     INTEGER,
    created_when      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_collection_run_requests_order PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS collection_run_stack_requests_order
(
    id                UUID NOT NULL,
    collection_run_id UUID,
    request_id        UUID,
    request_name      VARCHAR(255),
    request_order     INTEGER,
    created_when      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_collection_run_stack_requests_order PRIMARY KEY (id)
);
