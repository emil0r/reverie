CREATE TABLE admin_storage (
       k text not null default '',
       v text not null default ''
);

ALTER TABLE admin_storage ADD CONSTRAINT admin_storage_unique_k UNIQUE(k);
CREATE INDEX admin_storage_index_k ON admin_storage (k);
