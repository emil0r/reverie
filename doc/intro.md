# Introduction to reverie

## Templates
A template defines the HTML for a page and defines the areas within
the template. 

## Areas
An area is a place inside an HTML document where objects can be
placed. Defined as (area :name-of-area) inside a template. It
allows objects to be placed inside the area and moved around between
different pages and areas.

## Objects
An object has properties and returns rendered HTML. It can
differentiate between different request methods and have an approriate
function to be run for each method.

## Apps
An app defines a leaf in the structure tree that takes over the
handling of the uri after the path that is defined by the structure
tree.


## Modules
A module is something you wish to encapsulate. There is an automatic admin interface. Everything can be overridden in the interface and it can be skipped automatically.
