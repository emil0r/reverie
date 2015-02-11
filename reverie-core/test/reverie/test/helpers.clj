(ns reverie.test.helpers
  (:require [hiccup.core :as hiccup]
            [reverie.core :refer [deftemplate area defapp]]
            [reverie.page :as page]))


(defmacro with-noir
  "Executes the body within the context of Noir's bindings"
  [& body]
  `(binding [session/*noir-session* (atom {})
             session/*noir-flash* (atom {})
             cookies/*new-cookies* (atom {})
             cookies/*cur-cookies* (atom {})]
     ~@body))


(defn- template-render [request page properties params]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (page/name page)]]
   [:body
    [:div (area a)]
    [:div (area b)]]])

(defn expected [title a b]
  (hiccup/html
   [:html
    [:head
     [:meta {:charset "UTF-8"}]
     [:title title]]
    [:body
     [:div a]
     [:div b]]]))

(defn- baz-get [request page properties {:keys [caught]}]
  {:a (str "baz get " caught)
   :b (str "baz get " caught)})
(defn- baz-post [request page properties {:keys [caught]}]
  {:a (str "baz post " caught)
   :b (str "baz post " caught)})
(defn- baz-any [request page properties {:keys [caught]}]
  {:a (str "baz any " caught)
   :b (str "baz any " caught)})

(deftemplate foobar template-render)
(deftemplate foobaz template-render)
(defapp baz {} [["/" {:get baz-get :post baz-post :any baz-any}]
                ["/:caught" {:get baz-get :post baz-post :any baz-any}]])
