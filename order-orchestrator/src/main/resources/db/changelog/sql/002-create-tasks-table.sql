-- changeset create-tasks-table-:002

CREATE TABLE IF NOT EXISTS tasks
(
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id        UUID not null references orders(id),
    status          numeric not null,
    result          numeric,
    step            numeric not null,
    attempts        numeric not null,
    next_attempt_at timestamp with time zone,
    locked_until    timestamp with time zone,
    version         bigint not null default 0,
    created_at      timestamp with time zone not null,
    updated_at      timestamp with time zone not null
);

create unique index tasks_order_id_i on tasks(order_id);
create index tasks_status_next_attempt_at_i on tasks(status, next_attempt_at);
create index tasks_status_locked_until_i on tasks(status, locked_until);
