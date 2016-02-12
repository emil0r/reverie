# Templates

A template defines the HTML for a page and defines the areas within the template. Templates are always used by the reverie.page/Page implementation and can be used by both the reverie.page/AppPage and reverie.page/RawPage implementations.

reverie/CMS endorses [hiccup](https://github.com/weavejester/hiccup) for HTML rendering, but it is quite possible to use another library in addittion to hiccup. [enlive](https://github.com/cgrand/enlive) has successfully been used in the past. See render-fn in [init.clj](../reverie/init.clj.md).


```clojure
(ns some-namespace
  (:require [hiccup.page :refer [html5]]
            [reverie.core :refer [deftemplate]]))

;; define a template function. return anything that is ring compatible
(defn main-template [request page properties params]
  (html5
    [:head
      [:title "my main template"]]
    [:body
      "body"]))

;; define a template main and tell reverie to use the
;; function main-template for the template
(deftemplate main main-template)
```
