CREATE OR REPLACE VIEW reverie_page_view AS
WITH status AS (
  SELECT serial, MAX(version) AS version
  FROM reverie_page
  WHERE version <= 1
  GROUP BY serial
)
SELECT
  p.*,
  CASE
    WHEN status.version = 1 THEN 'p'
    WHEN status.version = 0 THEN 'u'
    WHEN status.version = -1 THEN 'd'
    ELSE 'u'
  END AS status,
  prop.data AS properties
FROM
  reverie_page p
  LEFT JOIN status ON p.serial = status.serial
  LEFT JOIN reverie_page_properties prop ON p.serial = prop.page_serial
WHERE
  p.version IN (-1, 0, 1);
