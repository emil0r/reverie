CREATE TABLE reverie_page (
    id serial primary key,
    serial integer NOT NULL,
    parent integer,
    created timestamp without time zone NOT NULL DEFAULT now(),
    updated timestamp without time zone NOT NULL,
    template character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    version integer NOT NULL,
    route character varying(2048) NOT NULL,
    type character varying(100) NOT NULL,
    app character varying(100) NOT NULL DEFAULT '',
    "order" integer NOT NULL
);

CREATE TABLE reverie_page_properties (
    id serial primary key,
    created timestamp without time zone NOT NULL DEFAULT now(),
    name character varying(255) NOT NULL,
    key character varying(100) NOT NULL,
    value character varying(255) NOT NULL,
    page_id integer NOT NULL references reverie_page(id)
);

CREATE TABLE reverie_object (
       id serial primary key,
       created timestamp without time zone NOT NULL DEFAULT now(),
       updated timestamp without time zone NOT NULL,
       name character varying(255) NOT NULL,
       area character varying(100) NOT NULL,
       route character varying(1024) NOT NULL DEFAULT '',
       "order" integer NOT NULL,
       page_id integer NOT NULL references reverie_page(id)
);


CREATE TABLE reverie_role (
    id serial primary key,
    created timestamp without time zone DEFAULT now() NOT NULL,
    name character varying(255) NOT NULL
);



CREATE INDEX object_index_id ON reverie_object USING btree (id);
CREATE INDEX page_index_name ON reverie_page USING btree (name);
CREATE INDEX page_index_parent ON reverie_page USING btree (parent);
CREATE INDEX page_index_serial ON reverie_page USING btree (serial);
CREATE INDEX page_index_route ON reverie_page USING btree (route);

ALTER TABLE reverie_page ADD CONSTRAINT page_unique_serial UNIQUE(serial,route,version);
