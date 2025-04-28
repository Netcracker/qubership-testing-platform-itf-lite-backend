CREATE TABLE IF NOT EXISTS actions (
    id            UUID NOT NULL,
    name          varchar(255),
    created_when  timestamp default CURRENT_TIMESTAMP,
    modified_when timestamp default CURRENT_TIMESTAMP,
    description   varchar(255),
    deprecated    boolean default false
);