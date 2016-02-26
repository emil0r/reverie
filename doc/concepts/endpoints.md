# Endpoints

An endpoint is a URL structure defined by the programmer where the programmer has full control over the output. Sample use case would be a simple API. They are very similar to apps with the distinction that the webmaster can't touch them.


## areas

Areas are not supported for endpoints.

## templates

Templates are supported by apps. The way to use them is to return a hashmap with the areas defined as keys. Template support is limited by the keys having to exactly match up against the areas in the template.

## more info

For more info look at [reverie.endpoint](../reverie/endpoint.md)
