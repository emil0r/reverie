CREATE TABLE reverie_page (
    id serial primary key,
    serial integer NOT NULL,
    parent integer,
    template character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    version integer NOT NULL,
    path character varying(2048) NOT NULL,
    created timestamp without time zone DEFAULT now() NOT NULL,
    updated timestamp without time zone NOT NULL,
    type character varying(100) NOT NULL,
    app character varying(100) DEFAULT ''::character varying NOT NULL,
    "order" integer NOT NULL
);

CREATE TABLE reverie_page_properties (
    id serial primary key,
    created timestamp without time zone DEFAULT now() NOT NULL,
    name character varying(255) NOT NULL,
    key character varying(100) NOT NULL,
    value character varying(255) NOT NULL,
    page_id integer NOT NULL references reverie_page(id)
);

CREATE TABLE reverie_object (
       id serial primary key,
       serial integer NOT NULL,
       created timestamp without time zone DEFAULT now() NOT NULL,
       updated timestamp without time zone NOT NULL,
       name character varying(255) NOT NULL,
       area character varying(100) NOT NULL,
       "order" integer NOT NULL,
       page_id integer NOT NULL references reverie_page(id)
);


CREATE TABLE reverie_role (
    id serial primary key,
    created timestamp without time zone DEFAULT now() NOT NULL,
    name character varying(255) NOT NULL
);
