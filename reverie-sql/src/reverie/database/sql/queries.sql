-- name: add-page<!
INSERT INTO reverie_page VALUES (default, :serial, :parent, default, default, :template, :name, :title, :version, :slug, '', :type, :app, :order);

-- name: add-object<!
INSERT INTO reverie_object VALUES (default, default, default, :name, :area, :route, :order, default, :page_id);

-- name: copy-page<!
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

-- name: copy-object-meta<!
INSERT INTO
       reverie_object (name, area, route, "order", version, page_id)
SELECT
       name, area, route, "order", version, :pageid
FROM
       reverie_object
WHERE
       id = :id;


-- name: update-published-pages-order!
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
