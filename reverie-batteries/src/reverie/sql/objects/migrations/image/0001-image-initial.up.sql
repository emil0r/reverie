CREATE TABLE batteries_image (
       id BIGSERIAL PRIMARY KEY,
       object_id BIGINT NOT NULL REFERENCES reverie_object(id),
       title varchar(100) NOT NULL DEFAULT '',
       alt varchar(100) NOT NULL DEFAULT '',
       src varchar(255) NOT NULL DEFAULT '',
       height integer NULL,
       width integer NULL
);
