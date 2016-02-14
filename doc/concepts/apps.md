# Apps

An app is a normal page in the CMS which takes care of its own URL handling.

## Example routing

Let's say you wish to build a webstore, and you decide to use an app for this. You need multiple views and you want pretty URLs for this. We decide that the below URL structure is what we want.

- /store (this is handled by the webmaster)
  - /
  - /categories
  - /category/:category
  - /category/:category/:sub-category
  - /product/:product-name
  - /search
  - /checkout
  - /checkout/:step


With apps you define all the routes you wish to handle with respective handlers, and then hand it over to the webmaster to define where in the overall URL structure it should go. In addition apps supports areas, allowing for the webmaster to add additional information to the page as they see fit.


## areas

Areas are supported, giving webmasters the option of adding objects to the page. Technically the URL routing is also fully supported (ie, you can have an object show up at /category/:category in the above example), but the admin interface in its current form does not support this.


## templates

Templates are supported by apps. The way to use them is to return a hashmap with the areas defined as keys. Currently template support is limited by the keys having to exactly match up against the areas in the template.

## more info

For more info look at [reverie.app](../reverie/app.md)
