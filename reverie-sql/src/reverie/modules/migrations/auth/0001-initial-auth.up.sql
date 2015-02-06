CREATE TABLE reverie_role (
    id bigserial primary key,
    created timestamp with time zone DEFAULT now() NOT NULL,
    name text NOT NULL
);
