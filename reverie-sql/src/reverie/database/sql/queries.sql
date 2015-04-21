-- name: sql-add-page<!
INSERT INTO reverie_page VALUES (default, :serial, :parent, default, default, :template, :name, :title, :version, :slug, '', :type, :app, :order);

-- name: sql-add-object<!
INSERT INTO reverie_object VALUES (default, default, default, :name, :area, :route, :order, default, :page_id);

-- name: sql-copy-page<!
INSERT INTO
       reverie_page (serial, parent, template, name,
                     title, slug, route, type, app, "order", version)
SELECT
       serial, parent, template, name, title,
       slug, route, type, app, "order", 1
FROM
       reverie_page
WHERE
       id = :id;

-- name: sql-copy-object-meta<!
INSERT INTO
       reverie_object (name, area, route, "order", version, page_id)
SELECT
       name, area, route, "order", version, :pageid
FROM
       reverie_object
WHERE
       id = :id;


-- name: sql-update-published-pages-order!
UPDATE
     reverie_page
SET
     "order" = pu.order
FROM (      SELECT serial, "order"
            FROM reverie_page
            WHERE parent = :parent AND version = 0      ) AS pu
WHERE
     version = 1
     AND reverie_page.serial = pu.serial;


-- name: sql-get-pages-1
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), menu AS (
   SELECT page_serial, value
   FROM reverie_page_properties
   WHERE key = 'menu'
)
SELECT
        p.*,
        COALESCE(pub.version, 0) AS published_p,
        m.value AS menu
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN menu m ON p.serial = m.page_serial
WHERE
        p.version = 0
        OR p.version = 1
ORDER BY
        p.order;


-- name: sql-get-pages-2
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), menu AS (
   SELECT page_serial, value
   FROM reverie_page_properties
   WHERE key = 'menu'
)
SELECT
        p.*,
        COALESCE(pub.version, 0) AS published_p,
        m.value AS menu
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN menu m ON p.serial = m.page_serial
WHERE
        p.version = :version
ORDER BY
        p.order;


-- name: sql-get-page-1
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), menu AS (
   SELECT page_serial, value
   FROM reverie_page_properties
   WHERE key = 'menu'
)
SELECT
        p.*,
        COALESCE(pub.version, 0) AS published_p,
        m.value AS menu
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN menu m ON p.serial = m.page_serial
WHERE
        p.id = :id;

-- name: sql-get-page-2
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), menu AS (
   SELECT page_serial, value
   FROM reverie_page_properties
   WHERE key = 'menu'
)
SELECT
        p.*,
        COALESCE(pub.version, 0) AS published_p,
        m.value AS menu
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN menu m ON p.serial = m.page_serial
WHERE
        p.serial = :serial
        AND p.version = :version;

-- name: sql-get-page-children
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), menu AS (
   SELECT page_serial, value
   FROM reverie_page_properties
   WHERE key = 'menu'
)
SELECT
        p.*,
        COALESCE(pub.version, 0) AS published_p,
        m.value AS menu
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN menu m ON p.serial = m.page_serial
WHERE
        p.parent = :parent
        AND p.version = :version
ORDER BY
      p.order;
