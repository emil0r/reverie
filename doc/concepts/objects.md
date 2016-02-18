# Objects

An object is a representation of something you wish to have rendered as HTML and/or javascript. Objects have properties, can be placed in areas, support migrations, multiple input types and can differentiate between request methods.

## Examples

- Image object
  - Has an image property which allows the web master to pick an image.
  - The image has to always render, so the object sets the render function to always run, regardless of request method.
  - The web master can add CSS classes through a text property.

- Personnel object
  - Has a dropdown property which draws from an LPAD database
  - Has an optional header property
  - When rendered the object draws from the LDAP database, caches the result for 10 minutes and then renders the data. In the event the header object is there it renders it
  
- Raw object
  - A naughty object which renders whatever is put in **exactly as is**. Introduced because the web masters needed to put in some javascript to support some third party services they enlisted in a few of the pages they are taking care of.
  
  
The common theme among all the examples is that you have something you wish to represent, you want to be able to manipulate that something to various degrees and you want to have a say in which page uses the object and where on the page the object goes.

## more info

For more info see [reverie.object](../reverie/object.md)
