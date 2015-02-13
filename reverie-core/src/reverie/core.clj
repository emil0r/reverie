(ns reverie.core
  (:require [clojure.string :as str]
            [reverie.area :as a]
            [reverie.module :as module]
            [reverie.module.entity :as entity]
            reverie.modules.default
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.system :as sys]
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
  (let [name (keyword name)
        migration (:migration options)]
    `(do
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! sys/storage assoc-in [:apps ~name]
              {:app-routes (map route/route ~routes)
               :options ~options}))))

(defmacro defpage [path options routes]
  (let [properties {:name path :type :raw}
        migration (:migration options)]
    `(do
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! site/routes assoc ~path [(route/route [~path]) ~properties])
       (swap! sys/storage assoc-in [:raw-pages ~path]
              {:routes (map route/route ~routes)
               :options ~options}))))

(defmacro defmodule [name options & [routes]]
  (let [name (keyword name)
        interface? (:interface? options)
        migration (:migration options)
        path (str "/admin/frame/module/" (clojure.core/name name))]
    `(do
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (when ~interface?
         (swap! site/routes assoc ~path
                [(route/route [~path]) {:name ~name
                                        :path ~path
                                        :type :module}]))
       (swap! sys/storage assoc-in [:modules ~name]
              {:options ~options
               :name ~name
               :module (module/module
                        ~name
                        (map entity/module-entity (:entities ~options))
                        ~options
                        (map route/route (if ~interface?
                                           (:module-default-routes @sys/storage)
                                           ~routes)))}))))

(defmacro defobject [name options methods]
  (let [name (keyword name)
        migration (:migration options)]
    `(do
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! sys/storage assoc-in [:objects ~name]
             {:options ~options
              :methods ~methods
              :table (keyword
                      (or (get ~options :table)
                          (str/replace ~name #"/|\." "_")))}))))
