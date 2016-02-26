# App

Technical documentation for App.

```clojure
(ns some-namespace
  (:require [reverie.core :refer [defapp]]))
  
  
(defn level2 [request page properties {:keys [caught]}]
  {:a (str "I caught myself " caught)})

(defn level1 [request page properties params]
  {:a "I don't do much..."})


(defapp
  ;; name of the app
  myapp 

  ;; options
  {
   ;; load i18n dictionary
   :i18n "path/to/edn-file.edn" ;; can be a hashmap as well
   
   ;; run migrations
   :migrations {:path "path/to/migration.sql"
                :automatic? true}
                
   ;; headers map
   :headers {"Content-Type" application/xhtml+xml; charset=utf-8;"}

  } 
  
  ;; routes
  [[
    ;; the route
    "/" 
    ;; :any request method will satisfy
    {:any level1}]
    
   ;; catch all
   ["/:caught" {:any level2}]])


```


## routes

See [reverie.route.md](route.md) for more info on routes.
