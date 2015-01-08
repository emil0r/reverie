-- pages
INSERT INTO reverie_page VALUES (1, 1, NULL, now(), now(), 'foobar', 'Main', '', 0, '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (2, 1, NULL, now(), now(), 'foobar', 'Main', '', 1, '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (3, 2, 1, now(), now(), 'foobaz', 'Baz', '', 0, '/baz', 'app', 'baz', 1);
INSERT INTO reverie_page VALUES (4, 3, 1, now(), now(), 'foobaz', 'Bar', '', 0, '/bar', 'page', '', 2);
-- objects
INSERT INTO reverie_object VALUES(default, now(), now(), 'text', 'a', '', 1, 1);
INSERT INTO reverie_object VALUES(default, now(), now(), 'text', 'a', '', 1, 2);
INSERT INTO reverie_object VALUES(default, now(), now(), 'text', 'a', '', 1, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'image', 'b', '', 2, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'text', 'a', '', 1, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'text', 'a', '', 2, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'text', 'a', '', 3, 4);
