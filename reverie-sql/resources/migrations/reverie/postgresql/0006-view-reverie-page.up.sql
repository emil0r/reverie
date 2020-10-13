CREATE OR REPLACE VIEW view_reverie_page AS
WITH published AS (
  SELECT serial, version
  FROM reverie_page
  WHERE version = 1
)
SELECT
  p.*,
  COALESCE(pub.version, 0) = 1 AS published_p,
  prop.data AS properties
FROM
  reverie_page p
  LEFT JOIN published pub ON p.serial = pub.serial
  LEFT JOIN reverie_page_properties prop ON p.serial = prop.page_serial
WHERE
  p.version IN (0, 1);
