CREATE TABLE batteries_raw (
       id serial primary key not null,
       object_id bigserial references reverie_object(id) not null,
       text text not null default ''
);
