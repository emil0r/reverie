(ns reverie.core
  (:require [clojure.string :as str]
            [reverie.area :as a]
            [reverie.i18n :as i18n]
            [reverie.helpers.middleware :refer [wrap-response-with-handlers]]
            [reverie.module :as module]
            [reverie.module.entity :as entity]
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.system :as sys]
            [reverie.template :as template]
            [reverie.util :as util]
            reverie.AreaException)
  (:import [reverie AreaException]))

(defmacro area
  ([name]
   (let [name (if (symbol? name) (keyword name) name)
         params (keys &env)]
     (cond
      (some #(= name %) [:body :headers :status])
      (throw (AreaException. "areas can't be named body, headers or status"))
      (and (some #(= 'request %) params)
           (some #(= 'page %) params))
      `(render/render (a/area (keyword ~name)) ~'request ~'page)
      :else (throw (AreaException. "area assumes variables 'request' and 'page' to be present. If you wish to use other named variables send them after the name of the area like this -> (area :a req p)")))))
  ([name display]
   (let [name (if (symbol? name) (keyword name) name)
         display (if (symbol? display) (keyword display) display)
         params (keys &env)]
     (cond
      (some #(= name %) [:body :headers :status])
      (throw (AreaException. "areas can't be named body, headers or status"))
      (and (some #(= 'request %) params)
           (some #(= 'page %) params))
      `(render/render (a/area ~(keyword name) ~(keyword display)) ~'request ~'page)
      :else (throw (AreaException. "area assumes variables 'request' and 'page' to be present. If you wish to use other named variables send them after the name of the area like this -> (area :a req p)")))))
  ([name request page]
   (let [name (if (symbol? name) (keyword name) name)]
     `(render/render (a/area (keyword ~name)) ~request ~page)))
  ([name display request page]
   (let [name (if (symbol? name) (keyword name) name)
         display (if (symbol? display) (keyword display) display)]
     `(render/render (a/area (keyword ~name) (keyword ~display)) ~request ~page))))

(defmacro deftemplate [name function]
  (let [name (keyword name)]
    `(do (swap! sys/storage assoc-in [:templates ~name]
                (template/template ~function))
         nil)))

(defmacro defapp [name options routes]
  (when (not (true? (:disabled? options)))
    (let [name (keyword name)
          migration (assoc (:migration options) :type :app)
          renderer (:renderer options)]
      (if-not (nil? renderer)
        (do (assert (util/namespaced-kw? renderer) ":renderer must be a namespaced keyword")
            (assert (not (nil? (sys/renderer renderer))) (format "Renderer %s has not yet been defined." (util/kw->str renderer)))))
      `(do
         (i18n/load-from-options! ~options)
         (when ~migration
           (swap! sys/storage assoc-in [:migrations :app ~name] ~migration))
         (swap! sys/storage assoc-in [:apps ~name]
                {:app-routes (map route/route ~routes)
                 :options ~options})
         nil))))

(defmacro defpage
  "Define a page directly into the tree structure of the site"
  [path options routes]
  (when (not (true? (:disabled? options)))
    (let [properties {:name path :type :raw}
          migration (assoc (:migration options) :type :raw-page)
          renderer (:renderer options)
          middleware (if-let [handlers (:middleware options)]
                       (wrap-response-with-handlers handlers))]
      (if-not (nil? renderer)
        (do (assert (util/namespaced-kw? renderer) ":renderer must be a namespaced keyword")
            (assert (not (nil? (sys/renderer renderer))) (format "Renderer %s has not yet been defined." (util/kw->str renderer)))))
      `(do
         (i18n/load-from-options! ~options)
         (when ~migration
           (swap! sys/storage assoc-in [:migrations :raw-page ~path] ~migration))
         (swap! site/routes assoc ~path [(route/route [~path]) ~properties])
         (swap! sys/storage assoc-in [:raw-pages ~path]
                {:routes (map route/route ~routes)
                 :options (assoc ~options :middleware ~middleware)})
         nil))))
(defn undefpage
  "Undefine a page"
  [path]
  (swap! sys/storage assoc-in [:migrations :raw-page path] nil)
  (swap! sys/storage assoc-in [:raw-pages path] nil)
  (swap! site/routes dissoc path)
  nil)

(defmacro defmodule [name options & [routes]]
  (when (not (true? (:disabled? options)))
    (let [name (keyword name)
          interface? (:interface? options)
          migration (assoc (:migration options) :type :module)
          path (str "/admin/frame/module/" (or (:slug options) (util/slugify name)))
          renderer (:renderer options)]
      (if-not (nil? renderer)
        (do (assert (util/namespaced-kw? renderer) ":renderer must be a namespaced keyword")
            (assert (not (nil? (sys/renderer renderer))) (format "Renderer %s has not yet been defined." (util/kw->str renderer)))))
      `(do
         (i18n/load-from-options! ~options)
         (when ~migration
           (swap! sys/storage assoc-in [:migrations :module ~name] ~migration))
         (swap! site/routes assoc ~path
                [(route/route [~path]) {:name ~name
                                        :path ~path
                                        :type :module}])
         (swap! sys/storage assoc-in [:modules ~name]
                {:options ~options
                 :name ~name
                 :module (module/module
                          ~name
                          (map entity/module-entity (:entities ~options))
                          ~options
                          (map route/route (if ~interface?
                                             (vec
                                              (concat
                                               ~routes
                                               (:module-default-routes @sys/storage)))
                                             ~routes)))})
         nil))))

(defmacro defobject [name options methods]
  (when (not (true? (:disabled? options)))
    (let [name (keyword name)
          migration (assoc (:migration options) :type :object)
          renderer (:renderer options)]
      (if-not (nil? renderer)
        (do (assert (util/namespaced-kw? renderer) ":renderer must be a namespaced keyword")
            (assert (not (nil? (sys/renderer renderer))) (format "Renderer %s has not yet been defined." (util/kw->str renderer)))))
      `(do
         (i18n/load-from-options! ~options)
         (when ~migration
           (swap! sys/storage assoc-in [:migrations :object ~name] ~migration))
         (swap! sys/storage assoc-in [:objects ~name]
                {:options ~options
                 :methods ~methods
                 :table (keyword
                         (or (get ~options :table)
                             (str/replace ~name #"/|\." "_")))})
         nil))))

(defmacro defrenderer
  "Define a renderer which separates the rendering from the computation of an object/page"
  [name options & [methods]]
  (when (not (true? (:disabled? options)))
    (assert (util/namespaced-kw? name) "Name must be a namespaced keyword")
    (if-not (nil? methods)
      (assert (map? methods) "methods, if supplied, must be a hash-map"))
    (let [override (:override options)]
      ;; check for an override and the validity of the name
      (if-not (nil? override)
        (assert (util/namespaced-kw? override) "Override must be a namespaced keyword"))
      `(do
         (i18n/load-from-options! ~options)
         (swap! sys/storage assoc-in [:renderers ~name]
                (render/map->Renderer {:name ~name :options ~options :methods-or-routes ~methods}))
         ;; have we designed the renderer to be overriding another renderer?
         (if-not (nil? ~override)
           (swap! sys/storage assoc-in [:renderers :reverie.system/override ~override] ~name))
         nil))))

(defn undefrenderer
  "Undefine a renderer"
  [name]
  (assert (util/namespaced-kw? name) "Name must be a namespaced keyword")
  (let [renderer (get-in @sys/storage [:renderers name])
        override (get-in renderer [:options :override])]
    (do (swap! sys/storage assoc-in [:renderers name] nil)
        (swap! sys/storage assoc-in [:renderers :reverie.system/override override] nil))
    nil))
