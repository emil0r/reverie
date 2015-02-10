CREATE TABLE auth_role (
    id serial primary key,
    created timestamp with time zone DEFAULT now() NOT NULL,
    name text NOT NULL
);

CREATE TABLE auth_user (
    id serial primary key,
    created timestamp with time zone DEFAULT now() NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    email text NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    last_login timestamp with time zone NOT NULL
);


CREATE TABLE auth_group (
    id serial primary key,
    name text NOT NULL
);

CREATE TABLE auth_user_group (
    user_id integer references auth_user(id),
    group_id integer references auth_group(id)
);

CREATE TABLE auth_user_role (
    user_id integer references auth_user(id),
    role_id integer references auth_role(id)
);


CREATE TABLE auth_group_role (
    group_id integer references auth_group(id),
    role_id integer references auth_role(id)
);
