-- pages
INSERT INTO reverie_page VALUES (default, 1, NULL, now(), now(), 'foobar', 'Main', '', 0, 'main', '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (default, 1, NULL, now(), now(), 'foobar', 'Main', '', 1, 'main', '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (default, 2, 1, now(), now(), 'foobaz', 'Baz', '', 0, 'baz', '/baz', 'app', 'baz', 1);
INSERT INTO reverie_page VALUES (default, 2, 1, now(), now(), 'foobaz', 'Baz', '', 1, 'baz', '/baz', 'app', 'baz', 1);
INSERT INTO reverie_page VALUES (default, 3, 1, now(), now(), 'foobaz', 'Bar', '', 0, 'bar', '/bar', 'page', '', 2);
INSERT INTO reverie_page VALUES (default, 3, 1, now(), now(), 'foobaz', 'Bar', '', 1, 'bar', '/bar', 'page', '', 2);
-- objects
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, 1);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, 2);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/image', 'b', '/caught-this', -1, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/image', 'b', '/caught-this', -1, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'b', '', 1, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'b', '', 1, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, 5);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, 6);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 2, 5);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 2, 6);
-- text
INSERT INTO batteries_text VALUES(default, 1, 'Text1');
INSERT INTO batteries_text VALUES(default, 2, 'Text1 (publ)');
INSERT INTO batteries_text VALUES(default, 3, 'Text2');
INSERT INTO batteries_text VALUES(default, 4, 'Text2 (publ)');
INSERT INTO batteries_text VALUES(default, 7, 'Text3');
INSERT INTO batteries_text VALUES(default, 8, 'Text3 (publ)');
INSERT INTO batteries_text VALUES(default, 9, 'Text4');
INSERT INTO batteries_text VALUES(default, 10, 'Text4 (publ)');
INSERT INTO batteries_text VALUES(default, 11, 'Text5');
INSERT INTO batteries_text VALUES(default, 12, 'Text5 (publ)');


-- image
INSERT INTO batteries_image VALUES(default, 5, 'TitleImage1', 'AltImage1', '/path/to/img.jpg', NULL, NULL);
INSERT INTO batteries_image VALUES(default, 6, 'TitleImage1 (publ)', 'AltImage1 (publ)', '/path/to/img.jpg', NULL, NULL);
