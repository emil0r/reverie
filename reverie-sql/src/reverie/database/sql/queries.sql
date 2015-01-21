-- name: add-page<!
INSERT INTO reverie_page VALUES (default, :serial, :parent, default, default, :template, :name, :title, :version, :slug, :route, :type, :app, :order);

-- name: add-object<!
INSERT INTO reverie_object VALUES (default, default, default, :name, :area, :route, :order, :page_id);
