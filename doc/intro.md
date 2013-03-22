# Introduction to reverie

TODO: write [great documentation](http://jacobian.org/writing/great-documentation/what-to-write/)

## Templates
A template defines the HTML for a page and defines the areas within
the template. 

## Areas
An area is a place inside an HTML document where objects can be
placed. Defined as (area :name-of-area) inside a hiccup vector it
allows objects to be placed inside the area and moved around between
different pages and areas.

## Objects
An object has properties and returns an HTML snippet. It can
differentiate between different request methods and have an approriate
function to be run for each method.

## Apps
An app defines a leaf in the structure tree that takes over the
handling of the uri after the path that is defined by the structure
tree. It allows objects inside itself based on input. The app can use
a template as basis and will then simply fill the areas in the
template with its own HTML representation, define everything itself or
a combination thereof.


## Plugins
A plugin can have its own schema. Will have an automatic admin part.
