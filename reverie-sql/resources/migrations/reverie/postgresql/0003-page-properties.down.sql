ALTER TABLE reverie_page_properties DROP COLUMN data;
--;;
ALTER TABLE reverie_page_properties ALTER COLUMN key SET NOT NULL;
--;;
ALTER TABLE reverie_page_properties ALTER COLUMN value SET NOT NULL;
