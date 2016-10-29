ALTER TABLE auth_user ADD COLUMN token UUID NULL;
ALTER TABLE auth_user ADD COLUMN token_expire TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE auth_user ADD CONSTRAINT token_unique_constraint UNIQUE (token);
