# Endpoint

Technical documentation for Endpoint.

__Note__: The route handlers for defpage only takes three arguments compared to [deftemplate](../concepts/template.md), [defapp](app.md) and [defmodule](module.md).

```clojure
(ns some-namespace
  (:require [reverie.core :refer [defpage]]))
  
(defn level2-post [request page {:keys [caught]}]
  (str "You posted " caught))
  
(defn level2 [request page {:keys [caught]}]
  (str "I caught myself " caught))

(defn level1 [request page params]
  "I don't do much...")


(defpage
  ;; mounting point
  "/api/"  

  ;; options
  {
   ;; load i18n dictionary
   :i18n "path/to/edn-file.edn" ;; can be a hashmap as well
   
   ;; run migrations
   :migrations {:path "path/to/migration.sql"
                :automatic? true}

   ;; headers map
   :headers {"Content-Type" application/text; charset=utf-8;"}

  } 
  
  ;; routes
  [[
    ;; the route
    "/" 
    ;; :any request method will satisfy
    {:any level1}]
    
   ;; catch all
   ["/:caught" {:any level2 :post level2-post}]])


```


## routes

See [reverie.route.md](route.md) for more info on routes.
