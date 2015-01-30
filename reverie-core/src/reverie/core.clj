(ns reverie.core
  (:require [clojure.string :as str]
            [reverie.area :as a]
            [reverie.system :as sys]
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.template :as template])
  (:import [reverie AreaException]))

(defmacro area
  ([name]
     (let [name (keyword name)
           params (keys &env)]
       (if (and (some #(= 'request %) params)
                (some #(= 'page %) params))
         `(render/render (a/area ~name) ~'request ~'page)
         (throw (AreaException. "area assumes variables 'request' and 'page' to be present. If you wish to use other named variables send them after the name of the area like this -> (area :a req p)")))))
  ([name request page]
     (let [name (keyword name)]
       `(render/render (a/area ~name) ~request ~page))))


(defmacro deftemplate [name function]
  (let [name (keyword name)]
    `(swap! sys/storage assoc-in [:templates ~name]
            (template/template ~function))))

(defmacro defapp [name options routes]
  (let [name (keyword name)]
    `(swap! sys/storage assoc-in [:apps ~name]
            {:app-routes (map route/route ~routes)
             :options ~options})))

(defmacro defpage [path options routes]
  (let [properties {:name path :type :raw}]
    `(do
      (swap! site/routes assoc ~path [(route/route [~path]) ~properties])
      (swap! sys/storage assoc-in [:raw-pages ~path]
             {:routes (map route/route ~routes)
              :options ~options}))))

(defmacro defmodule [name options routes]
  (let [name (keyword name)]
    `(swap! sys/storage assoc-in [:modules ~name]
            {:routes (map route/route ~routes)
             :options ~options})))

(defmacro defobject [name options methods]
  (let [name (keyword name)
        migration (:migration options)]
    `(do
       (if ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! sys/storage assoc-in [:objects ~name]
             {:options ~options
              :methods ~methods
              :table (keyword
                      (or (get ~options :table)
                          (str/replace ~name #"/|\." "_")))}))))
