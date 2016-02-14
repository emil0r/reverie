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
  {} 
  
  ;; routes
  [[
    ;; the route
    "/" 
    ;; :any request method will satisfy
    {:any level1}]
    
   ;; catch all
   ["/:caught" {:any level2}]])


```
