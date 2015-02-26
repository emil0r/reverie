-- pages
INSERT INTO reverie_page VALUES (default, 1, NULL, now(), now(), 'foobar', 'Main', '', 0, 'main', '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (default, 1, NULL, now(), now(), 'foobar', 'Main', '', 1, 'main', '/', 'page', '', 1);
INSERT INTO reverie_page VALUES (default, 2, 1, now(), now(), 'foobaz', 'Baz', '', 0, 'baz', '/baz', 'app', 'baz', 1);
INSERT INTO reverie_page VALUES (default, 2, 1, now(), now(), 'foobaz', 'Baz', '', 1, 'baz', '/baz', 'app', 'baz', 1);
INSERT INTO reverie_page VALUES (default, 3, 1, now(), now(), 'foobaz', 'Bar', '', 0, 'bar', '/bar', 'page', '', 2);
INSERT INTO reverie_page VALUES (default, 3, 1, now(), now(), 'foobaz', 'Bar', '', 1, 'bar', '/bar', 'page', '', 2);
-- objects
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, default, 1);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, default, 2);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, default, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, default, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/image', 'b', '/caught-this', -1, default, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/image', 'b', '/caught-this', -1, default, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'b', '', 1, default, 3);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'b', '', 1, default, 4);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, default, 5);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 1, default, 6);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 2, default, 5);
INSERT INTO reverie_object VALUES(default, now(), now(), 'reverie/text', 'a', '', 2, default, 6);
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


-- role
INSERT INTO auth_role VALUES (default, default, 'admin');
INSERT INTO auth_role VALUES (default, default, 'staff');
INSERT INTO auth_role VALUES (default, default, 'user');

-- user
INSERT INTO auth_user VALUES (default, default, 'admin', 'bcrypt+sha512$400a78a96113cad1ea67c6ab$12$243261243132243057724e2e555876376a76626b79783156377569796554484551784e364d315a6a52557936446478486d337036496a6f6658425153', 'admin@admin.com', 'Admin', 'Admin', now(), true); -- password == admin

-- user roles
INSERT INTO auth_user_role VALUES (1, 1);

-- group
INSERT INTO auth_group VALUES (default, 'administrators');
INSERT INTO auth_group VALUES (default, '1');
INSERT INTO auth_group VALUES (default, '2');
INSERT INTO auth_group VALUES (default, '3');
INSERT INTO auth_group VALUES (default, '4');
INSERT INTO auth_group VALUES (default, '5');
INSERT INTO auth_group VALUES (default, '6');
INSERT INTO auth_group VALUES (default, '7');
INSERT INTO auth_group VALUES (default, '8');
INSERT INTO auth_group VALUES (default, '9');
INSERT INTO auth_group VALUES (default, '10');
INSERT INTO auth_group VALUES (default, '11');
INSERT INTO auth_group VALUES (default, '12');
INSERT INTO auth_group VALUES (default, '13');
INSERT INTO auth_group VALUES (default, '14');
INSERT INTO auth_group VALUES (default, '15');
INSERT INTO auth_group VALUES (default, '16');
INSERT INTO auth_group VALUES (default, '17');
INSERT INTO auth_group VALUES (default, '18');
INSERT INTO auth_group VALUES (default, '19');
INSERT INTO auth_group VALUES (default, '20');

INSERT INTO auth_user_group VALUES (1,1);
INSERT INTO auth_group_role VALUES (1,1);
INSERT INTO auth_group_role VALUES (1,2);
INSERT INTO auth_group_role VALUES (1,3);

INSERT INTO auth_storage VALUES ('reverie.page/Page', 1, null, 'user', 'view');
