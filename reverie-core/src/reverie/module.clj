(ns reverie.module
  (:require [reverie.auth :refer [with-access]]
            [reverie.module.entity :as entity]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.response :as response]
            [reverie.route :as route]
            [reverie.system :as sys]
            [reverie.util :as util])
  (:refer-clojure :exclude [list name])
  (:import [reverie ModuleException RenderException]))

(defprotocol IModuleDatabase
  (get-data
    [module entity params]
    [module entity params id])
  (save-data [module entity id data])
  (add-data [module entity data])
  (delete-data [module entity id] [module entity id cascade?]))

(defprotocol IModule
  (interface? [entity]
    "Should this be automatically interfaced?")
  (entities [module]
    "Entities of the module")
  (get-entity [module slug]
    "Get entity based on slug")
  (options [module]
    "Options of the module")
  (name [module]
    "Name of the module")
  (list [module entity] [module entity params offset limit]
    "List the fields in an entity in the admin interface")
  (filters [module entity]
    "Get possible filters")
  (slug [module]
    "Get slug to be used as part of a URI"))

(defrecord Module [name database entities options routes route]
  IModule
  (options [this] options)
  (entities [this] entities)
  (get-entity [this slug]
    (first (filter #(= slug (entity/slug %)) entities)))
  (name [this] (or (:name options) (clojure.core/name name)))
  (slug [this]
    (or (:slug options) (util/slugify name)))


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
  (properties [this] nil)
  (type [this] :module)
  (cache? [this] false)
  (version [this] 0))

(defn module [name entities options routes]
  (map->Module {:name name :entities entities
                :options options :routes routes}))
