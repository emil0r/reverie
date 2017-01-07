# Renderer

Renderers separate computations of objects/apps/modules/pages and the rendering of said computations. It allows for switching out the rendering engine for all objects/apps/modules/pages.



```clojure
(ns some-namespace
  (:require [reverie.core :refer [defobject defrenderer]]))
  
;; defrender requires a namespaced keyword
(defrenderer ::renderer {:render-fn :hiccup})

(defn test [request object properties params]
  [:div "Hi from any"])

(defn test-get [request object properties params]
  [:div "Hi from get"])

(defn test-post [request object properties params]
  [:div "Hi from post"])

(defobject test
  {:table "objects_test"
   :migration {:path "src/objects/reverie-test/migrations/test"
               :automatic? true}
   :render ::renderer
   :fields {}
   :sections []}
  {:any test :get test-get :post test-post})
```

With the above setup, all the methods for the object will render what is returned from the various functions associated with them using the hiccup library.

The render-fn option is extendable through a multimethod in reverie.render called get-render-fn. It expects one argument and will return the function used to render with. If no method is found for the multimethod, the default method will return what was given to it.


## apps, pages &amp; modules

If you want to use renderers for apps, pages or modules you use the following renderer

```clojure

;; this renderer will convert everything from any path into a HTML string
(defrenderer ::renderer {:render-fn :hiccup})

(defapp app-test
  {:renderer ::renderer}
  [["/" {:any index}]])
  
  
;; this renderer will convert everything to a HTML string using hiccup,
;; in addition it separates the calculations and the presentation
(defrenderer ::renderer-targeted {:render-fn :hiccup} {::index {:any present-index}})



;; the route called ::index will use this function and returns a map with the data
(defn index [request page properties params]
 {:what "Index"})
 
 ;; this function is then used by the renderer to render the data it got from the index function
(defn present-index [data]
  [:div "We used: " (:what data)])

(defapp app-test
  {:renderer ::renderer-targeted}
  [["/" ^:meta {:name ::index} {:any index}]])
```


## override

It's possible to override already existing renderers with new renderers that either replace all functions, or just some parts.

```clojure
;; we define a renderer to be used in an app. it holds methods for the routes ::index and ::list
(defrenderer ::renderer 
             {:render-fn :hiccup} 
             {::index present-index ::list present-list})

;; we define a new renderer that is set to override ::renderer
;; we only provide an override for the route ::index
;; this means that ::list will still be using the methods from ::renderer
(defrenderer ::renderer-override 
             {:render-fn :hiccup
              :override ::renderer}
             {::index override-present-index})

```
