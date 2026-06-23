-- changeset init-orders-:001

-- TODO остальные поля, необходимые индексы
CREATE TABLE IF NOT EXISTS orders
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    address             TEXT,
    status              numeric not null,
    client_estimate     numeric(19,2) not null,
    final_amount        numeric(19,2),
    authorized_amount   numeric(19,2),
    captured_amount     numeric(19,2),
    failure_reason      text,
    failure_code         text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);