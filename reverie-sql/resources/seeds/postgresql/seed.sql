-- pages
INSERT INTO reverie_page VALUES (1, 1, NULL, now(), now(), 'foobar', 'Main', '', 0, '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (2, 1, NULL, now(), now(), 'foobar', 'Main', '', 1, '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (3, 2, 1, now(), now(), 'foobaz', 'Baz', '', 0, '/baz', 'app', 'baz', 1);
INSERT INTO reverie_page VALUES (4, 3, 1, now(), now(), 'foobaz', 'Bar', '', 0, '/bar', 'page', '', 2);
-- objects
INSERT INTO reverie_object VALUES(1, now(), now(), 'reverie/text', 'a', '', 1, 1);
INSERT INTO reverie_object VALUES(2, now(), now(), 'reverie/text', 'a', '', 1, 2);
INSERT INTO reverie_object VALUES(3, now(), now(), 'reverie/text', 'a', '', 1, 3);
INSERT INTO reverie_object VALUES(4, now(), now(), 'reverie/image', 'b', '/caught-this', -1, 3);
INSERT INTO reverie_object VALUES(5, now(), now(), 'reverie/text', 'b', '', 1, 3);
INSERT INTO reverie_object VALUES(6, now(), now(), 'reverie/text', 'a', '', 1, 4);
INSERT INTO reverie_object VALUES(7, now(), now(), 'reverie/text', 'a', '', 2, 4);
-- text
INSERT INTO batteries_text VALUES(default, 1, 'Text1');
INSERT INTO batteries_text VALUES(default, 2, 'Text1 (publ)');
INSERT INTO batteries_text VALUES(default, 3, 'Text2');
INSERT INTO batteries_text VALUES(default, 5, 'Text3');
INSERT INTO batteries_text VALUES(default, 6, 'Text4');
INSERT INTO batteries_text VALUES(default, 7, 'Text5');

-- image
INSERT INTO batteries_image VALUES(default, 4, 'TitleImage1', 'AltImage1', '/path/to/img.jpg', NULL, NULL);
