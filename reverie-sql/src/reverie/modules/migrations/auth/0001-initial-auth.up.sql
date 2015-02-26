CREATE TABLE auth_role (
    id serial primary key,
    created timestamp with time zone DEFAULT now() NOT NULL,
    name text NOT NULL
);
ALTER TABLE auth_role ADD CONSTRAINT auth_role_unique_name UNIQUE(name);

CREATE TABLE auth_user (
    id serial primary key,
    created timestamp with time zone DEFAULT now() NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    email text NOT NULL,
    spoken_name text NOT NULL,
    full_name text NOT NULL,
    last_login timestamp with time zone NULL,
    active_p boolean NOT NULL default true
);
ALTER TABLE auth_user ADD CONSTRAINT auth_user_unique_username UNIQUE(username);


CREATE TABLE auth_group (
    id serial primary key,
    name text NOT NULL
);
ALTER TABLE auth_group ADD CONSTRAINT auth_group_unique_name UNIQUE(name);


CREATE TABLE auth_user_group (
    user_id integer references auth_user(id) ON DELETE CASCADE,
    group_id integer references auth_group(id) ON DELETE CASCADE
);
ALTER TABLE auth_user_group ADD CONSTRAINT auth_user_group_unique UNIQUE(user_id, group_id);


CREATE TABLE auth_user_role (
    user_id integer references auth_user(id) ON DELETE CASCADE,
    role_id integer references auth_role(id) ON DELETE CASCADE
);
ALTER TABLE auth_user_role ADD CONSTRAINT auth_user_role_unique UNIQUE(user_id, role_id);


CREATE TABLE auth_group_role (
    group_id integer references auth_group(id) ON DELETE CASCADE,
    role_id integer references auth_role(id) ON DELETE CASCADE
);
ALTER TABLE auth_group_role ADD CONSTRAINT auth_group_role_unique UNIQUE(group_id, role_id);


CREATE TABLE auth_storage (
    what text NOT NULL,
    id_int integer NULL,
    id_string text NULL,
    role text NOT NULL,
    action text NOT NULL
);
ALTER TABLE auth_storage ADD CONSTRAINT auth_storage_unique UNIQUE(what, id_int, id_string, role, action);
