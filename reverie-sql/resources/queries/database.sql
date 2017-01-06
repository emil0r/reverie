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
), properties AS (
   SELECT page_serial,
          ARRAY_AGG(concat(key, ':' , value)) AS value
   FROM
          reverie_page_properties
   GROUP BY
          page_serial
)
SELECT
        p.*,
        COALESCE(pub.version, 0) = 1 AS published_p,
        prop.value AS properties
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN properties prop ON p.serial = prop.page_serial
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
), properties AS (
   SELECT page_serial,
          ARRAY_AGG(concat(key, ':' , value)) AS value
   FROM
          reverie_page_properties
   GROUP BY
          page_serial
)
SELECT
        p.*,
        COALESCE(pub.version, 0) = 1 AS published_p,
        prop.value AS properties
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN properties prop ON p.serial = prop.page_serial
WHERE
        p.version = :version
ORDER BY
        p.order;


-- name: sql-get-page-1
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), properties AS (
   SELECT page_serial,
          ARRAY_AGG(concat(key, ':' , value)) AS value
   FROM
          reverie_page_properties
   GROUP BY
          page_serial
)
SELECT
        p.*,
        COALESCE(pub.version, 0) = 1 AS published_p,
        prop.value AS properties
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN properties prop ON p.serial = prop.page_serial
WHERE
        p.id = :id;

-- name: sql-get-page-2
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), properties AS (
   SELECT page_serial,
          ARRAY_AGG(concat(key, ':' , value)) AS value
   FROM
          reverie_page_properties
   GROUP BY
          page_serial
)
SELECT
        p.*,
        COALESCE(pub.version, 0) = 1 AS published_p,
        prop.value AS properties
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN properties prop ON p.serial = prop.page_serial
WHERE
        p.serial = :serial
        AND p.version = :version;

-- name: sql-get-page-children
WITH published AS (
     SELECT serial, version
     FROM reverie_page
     WHERE version = 1
), properties AS (
   SELECT page_serial,
          ARRAY_AGG(concat(key, ':' , value)) AS value
   FROM
          reverie_page_properties
   GROUP BY
          page_serial
)
SELECT
        p.*,
        COALESCE(pub.version, 0) = 1 AS published_p,
        prop.value AS properties
FROM
        reverie_page p
        LEFT JOIN published pub ON p.serial = pub.serial
        LEFT JOIN properties prop ON p.serial = prop.page_serial
WHERE
        p.parent = :parent
        AND p.version = :version
ORDER BY
      p.order;


--name: sql-get-user-by-token
WITH roles AS (
	SELECT
		ur.user_id, ARRAY_AGG(r.name) AS roles
	FROM
		auth_role r
		INNER JOIN auth_user_role ur ON r.id = ur.role_id
	GROUP BY
		ur.user_id
),
groups AS (
	SELECT
		ug.user_id, ARRAY[ARRAY_agg(g.name), ARRAY_AGG(r.name)] AS groups
	FROM
		auth_user_group ug
		INNER JOIN auth_group g ON ug.group_id = g.id
		LEFT JOIN auth_group_role gr ON gr.group_id = g.id
		INNER JOIN auth_role r ON gr.role_id = r.id
	GROUP BY
		ug.user_id
)
SELECT
	u.*, r.roles, g.groups AS groups
FROM
	auth_user u
	LEFT JOIN roles r ON u.id = r.user_id
	LEFT JOIN groups g ON u.id = g.user_id
WHERE
        u.token = :id;


--name: sql-get-user-by-id
WITH roles AS (
	SELECT
		ur.user_id, ARRAY_AGG(r.name) AS roles
	FROM
		auth_role r
		INNER JOIN auth_user_role ur ON r.id = ur.role_id
	GROUP BY
		ur.user_id
),
groups AS (
	SELECT
		ug.user_id, ARRAY[ARRAY_agg(g.name), ARRAY_AGG(r.name)] AS groups
	FROM
		auth_user_group ug
		INNER JOIN auth_group g ON ug.group_id = g.id
		LEFT JOIN auth_group_role gr ON gr.group_id = g.id
		INNER JOIN auth_role r ON gr.role_id = r.id
	GROUP BY
		ug.user_id
)
SELECT
	u.*, r.roles, g.groups AS groups
FROM
	auth_user u
	LEFT JOIN roles r ON u.id = r.user_id
	LEFT JOIN groups g ON u.id = g.user_id
WHERE
        u.id = :id;


--name: sql-get-user-by-email
WITH roles AS (
	SELECT
		ur.user_id, ARRAY_AGG(r.name) AS roles
	FROM
		auth_role r
		INNER JOIN auth_user_role ur ON r.id = ur.role_id
	GROUP BY
		ur.user_id
),
groups AS (
	SELECT
		ug.user_id, ARRAY[ARRAY_agg(g.name), ARRAY_AGG(r.name)] AS groups
	FROM
		auth_user_group ug
		INNER JOIN auth_group g ON ug.group_id = g.id
		LEFT JOIN auth_group_role gr ON gr.group_id = g.id
		INNER JOIN auth_role r ON gr.role_id = r.id
	GROUP BY
		ug.user_id
)
SELECT
	u.*, r.roles, g.groups AS groups
FROM
	auth_user u
	LEFT JOIN roles r ON u.id = r.user_id
	LEFT JOIN groups g ON u.id = g.user_id
WHERE
        u.email = :id;


--name: sql-get-user-by-username
WITH roles AS (
	SELECT
		ur.user_id, ARRAY_AGG(r.name) AS roles
	FROM
		auth_role r
		INNER JOIN auth_user_role ur ON r.id = ur.role_id
	GROUP BY
		ur.user_id
),
groups AS (
	SELECT
		ug.user_id, ARRAY[ARRAY_agg(g.name), ARRAY_AGG(r.name)] AS groups
	FROM
		auth_user_group ug
		INNER JOIN auth_group g ON ug.group_id = g.id
		LEFT JOIN auth_group_role gr ON gr.group_id = g.id
		INNER JOIN auth_role r ON gr.role_id = r.id
	GROUP BY
		ug.user_id
)
SELECT
	u.*, r.roles, g.groups AS groups
FROM
	auth_user u
	LEFT JOIN roles r ON u.id = r.user_id
	LEFT JOIN groups g ON u.id = g.user_id
WHERE
        u.username = :id;


--name: sql-get-users
WITH roles AS (
	SELECT
		ur.user_id, ARRAY_AGG(r.name) AS roles
	FROM
		auth_role r
		INNER JOIN auth_user_role ur ON r.id = ur.role_id
	GROUP BY
		ur.user_id
),
groups AS (
	SELECT
		ug.user_id, ARRAY[ARRAY_agg(g.name), ARRAY_AGG(r.name)] AS groups
	FROM
		auth_user_group ug
		INNER JOIN auth_group g ON ug.group_id = g.id
		LEFT JOIN auth_group_role gr ON gr.group_id = g.id
		INNER JOIN auth_role r ON gr.role_id = r.id
	GROUP BY
		ug.user_id
)
SELECT
	u.*, r.roles, g.groups AS groups
FROM
	auth_user u
	LEFT JOIN roles r ON u.id = r.user_id
	LEFT JOIN groups g ON u.id = g.user_id;
