CREATE TABLE reverie_page (
    id bigserial primary key,
    serial integer NOT NULL,
    parent integer, -- references serial which can be in more than one row
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    template character varying(255) NOT NULL,
    name text NOT NULL,
    title text NOT NULL,
    version integer NOT NULL,
    slug varchar NOT NULL,
    route text NOT NULL,
    type text NOT NULL,
    app text NOT NULL DEFAULT '',
    "order" integer NOT NULL
);

CREATE TABLE reverie_page_properties (
    id bigserial primary key,
    created timestamp with time zone NOT NULL DEFAULT now(),
    key text NOT NULL,
    value text NOT NULL,
    page_serial bigint NOT NULL
);

CREATE TABLE reverie_object (
    id bigserial primary key,
    created timestamp with time zone NOT NULL DEFAULT now(),
    updated timestamp with time zone NOT NULL DEFAULT now(),
    name text NOT NULL,
    area text NOT NULL,
    route text NOT NULL DEFAULT '',
    "order" integer NOT NULL,
    version integer NOT NULL default 0,
    page_id bigint NOT NULL references reverie_page(id)
);


CREATE TABLE reverie_publishing (
    id bigserial primary key,
    created timestamp with time zone DEFAULT now() NOT NULL,
    page_id bigint NOT NULL references reverie_page(id),
    publish_time timestamp with time zone NOT NULL
);



CREATE INDEX object_index_id ON reverie_object USING btree (id);
CREATE INDEX page_index_name ON reverie_page USING btree (name);
CREATE INDEX page_index_parent ON reverie_page USING btree (parent);
CREATE INDEX page_index_serial ON reverie_page USING btree (serial);
CREATE INDEX page_index_route ON reverie_page USING btree (route);

ALTER TABLE reverie_page ADD CONSTRAINT page_unique_serial UNIQUE(serial,route,version);



CREATE OR REPLACE FUNCTION get_route(start_id integer)
RETURNS TABLE(route text, id bigint)
        LANGUAGE sql
        AS $$
        WITH RECURSIVE transverse(slug, parent, id, iterator) AS (
             SELECT
                CASE WHEN parent IS NULL THEN '/' ELSE slug END, parent, id, 1 AS iterator
             FROM
                reverie_page
             WHERE
                id = start_id
        UNION ALL
             SELECT
                CASE WHEN p.parent IS NULL THEN '' ELSE p.slug END || '/' || t.slug,
                p.parent, p.id, 1 + iterator AS iterator
             FROM
                reverie_page p
                INNER JOIN transverse t ON serial = t.parent
             WHERE
                t.parent = p.serial
                AND p.version = 0
                AND (p.serial != p.parent OR p.parent IS NULL)
                AND iterator < 100
)
SELECT slug, id FROM transverse WHERE parent IS NULL OR iterator = 100
$$;


CREATE OR REPLACE FUNCTION get_serials_recursively(start_serial integer)
RETURNS TABLE(serial integer, parent integer)
        LANGUAGE sql
        AS $$
        WITH RECURSIVE transverse(serial, parent) AS (
             SELECT serial, parent
             FROM reverie_page
             WHERE start_serial = serial AND version = 0
        UNION ALL
              SELECT
                p.serial, p.parent
              FROM
                reverie_page p
                INNER JOIN transverse t ON t.serial = p.parent
              WHERE p.version = 0
)
SELECT serial, parent FROM transverse
ORDER BY serial
$$;
