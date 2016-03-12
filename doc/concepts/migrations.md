# Migrations

Migrations use the [joplin](https://github.com/juxt/joplin) library, which in turn uses [ragtime](https://github.com/weavejester/ragtime) for handling SQL migrations. As of this time reverie only supports .sql migrations.

## The .sql files

You will need a .up.sql file and a .down.sql file. They are applied alphabetically, so either name them according to <0001-9999>-what-are-we-doing-here.up.sql and <0001-9999>-what-are-we-doing-here.down.sql or according to a date naming scheme.

**The following fields are always required for** ***objects***: id and object_id. See below for what they should look like.

```sql

CREATE TABLE objects_myobject (
       id serial primary key not null,
       object_id bigserial not null references reverie_object(id),
       -- more fields here
);


```
