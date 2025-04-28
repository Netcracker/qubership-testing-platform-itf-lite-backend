create table if not exists folders
(
    id            uuid not null
    constraint folders_pkey
    primary key,
    created_when  timestamp default CURRENT_TIMESTAMP,
    modified_when timestamp default CURRENT_TIMESTAMP,
    name          varchar(255),
    parent_id     uuid,
    project_id    uuid
    );

create table if not exists requests
(
    id             uuid not null
    constraint requests_pkey
    primary key,
    created_when   timestamp default CURRENT_TIMESTAMP,
    modified_when  timestamp default CURRENT_TIMESTAMP,
    name           varchar(255),
    content        text,
    type           varchar(255),
    folder_id      uuid,
    http_method    varchar(255),
    project_id     uuid,
    transport_type varchar(255),
    url            varchar(255)
    );

create table if not exists request_headers
(
    id            uuid not null
    constraint request_headers_pkey
    primary key,
    created_when  timestamp default CURRENT_TIMESTAMP,
    modified_when timestamp default CURRENT_TIMESTAMP,
    name          varchar(255),
    description   varchar(255),
    key           varchar(255),
    value         text,
    request_id    uuid
    constraint fk644b1yxpctg4o8d01ejicwyyy
    references requests
    );

create table if not exists request_params
(
    id            uuid not null
    constraint request_params_pkey
    primary key,
    created_when  timestamp default CURRENT_TIMESTAMP,
    modified_when timestamp default CURRENT_TIMESTAMP,
    name          varchar(255),
    description   varchar(255),
    key           varchar(255),
    value         varchar(255),
    request_id    uuid
    constraint fkgospad6h535i7126kel228up6
    references requests
    );

