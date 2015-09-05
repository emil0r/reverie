(ns reverie.site.apps.baz
  (:require [reverie.core :refer [defapp]]))


(defn- baz-get [request page properties {:keys [caught]}]
  {:a (str "baz get " caught)
   :b (str "baz get " caught)})
(defn- baz-post [request page properties {:keys [caught]}]
  {:a (str "baz post " caught)
   :b (str "baz post " caught)})
(defn- baz-any [request page properties {:keys [caught]}]
  {:a (str "baz any " caught)
   :b (str "baz any " caught)})

(defapp baz {} [["/" {:get baz-get :post baz-post :any baz-any}]
                ["/:caught" {:get baz-get :post baz-post :any baz-any}]])
