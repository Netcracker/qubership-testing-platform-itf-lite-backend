CREATE TABLE IF NOT EXISTS cookies(
    id uuid not null constraint cookies_pkey primary key,
    domain varchar(255) not null,
    key varchar(255) not null,
    value text,
    user_id uuid,
    project_id uuid,
    execution_request_id uuid,
    test_run_id uuid,
    disabled boolean DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS cookies_userId_idx ON cookies(user_id);
CREATE INDEX IF NOT EXISTS cookies_executionRequestId_testRunId_ix ON cookies(execution_request_id, test_run_id);

ALTER TABLE folders
    ADD COLUMN IF NOT EXISTS disable_cookie_generation boolean DEFAULT FALSE;

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS disable_cookie_generation boolean DEFAULT FALSE;
