(ns reverie.module
  (:require [reverie.page :as page]
            [reverie.render :as render]
            [reverie.response :as response]
            [reverie.route :as route]
            [reverie.security :refer [with-access]]
            [reverie.system :as sys]
            [reverie.util :as util])
  (:refer-clojure :exclude [list])
  (:import [reverie ModuleException RenderException]))

(defprotocol IModule
  (interface? [entity]
    "Should this be automatically interfaced?")
  (entities [module]
    "Entities of the module"))


(defprotocol IModuleEntity
  (list [entity] [entity params offset limit]
    "List the fields in an entity in the admin interface")
  (pk [entity]
    "Get the primary key for the entity (eg, id)")
  (fields [entity]
    "Fields for the entity in the database")
  (data [entity]
    "Data for the entity")
  (get-filters [module]
    "Get possible filters")
  (related [entity]
    "Related entities and how they relate"))



(defrecord ModuleOptions [offset limit filters])

(defrecord Module [database entities options routes route]
  IModule
  render/IRender
  (render [this request]
    (let [request (merge request
                         {:shortened-uri (util/shorten-uri
                                          (:uri request) (:path route))})]
      (with-access
        (get-in request [:reverie :user]) (:required-roles options)
        (if-let [page-route (first (filter #(route/match? % request) routes))]
          (let [{:keys [request method]} (route/match? page-route request)
                resp (method request this (:params request))]
            (let [t (if (:template options)
                      (get (:templates @sys/storage) (:template options)))]
              (if (and t
                       (map? resp)
                       (not (contains? resp :status))
                       (not (contains? resp :body))
                       (not (contains? resp :headers)))
                (render/render t request (assoc this :rendered resp))
                resp)))
          (response/get 404)))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.module/Module")))

  page/IPage
  (properties [this] nil))



(defrecord ModuleEntity [data fields id-key]
  IModuleEntity
  (pk [this] (or (get data id-key)
                 (get data :id)
                 (let [ids (filter #(re-find #"id" (name %)) (keys data))]
                   (cond
                    ( ids) (throw (ModuleException. "More than one key found for entity [ModuleEntity/id]"))
                    (= (count ids) 1) (get data (first ids))
                    :else (throw (ModuleException. "No key found for entity [ModuleEntity/id]"))))))
  (fields [this] fields)
  (data [this] data))


(defn module-entity [[field options]]
  )

(defn module [name entities options routes]
  (map->Module {:name name :entities entities
                :options options :routes routes}))
