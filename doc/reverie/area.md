# reverie.area

All implementation details for areas are in src/reverie/area.clj. The area macro is defined in core.

```clojure
(ns some-namespace
  (:require [hiccup.page :refer [html5]]
            [reverie.core :refer [deftemplate area]]))
            
;; define a template function. return anything that is ring compatible
(defn main-template [request page properties params]
  (html5
    [:head
      [:title "my main template"]]
    [:body
      ;; area a is now defined here in the body
      (area a]))

;; define a template main and tell reverie to use the
;; function main-template for the template
(deftemplate main main-template)


```

__Note__: Objects are bound to the name of the area, which causes them to be weakly bound to an area. If you change the name of the area, any objects already bound to it will not show up unless you change the binding manually in the database. This can also cause a problem when changing between templates if care is not taken. **Best practice** is therefore to name the areas a, b, c, d, e, f, etc... in accordance with the alphabet for all templates.


## Arities
| Arity | Explanation |
| ---   | --- |
| [name]                      | name will show up as display. request and page is assumbed as available symbols in the function where the area macro is used          |
| [name display]              | name is used internally, display is what will show up. request and page is assumbed as available symbols in the function where the area macro is used |
| [name request page]         | name will show up as display. request and page are sent in instead of assuming their names |
| [name display request page] | name is used internally, display is what will show up. request and page are sent in instead of assuming their names |
