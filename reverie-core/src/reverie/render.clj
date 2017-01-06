(ns reverie.render
  "Render protocol and Renderer implementation"
  (:require [clojure.core.match :refer [match]]
            [hiccup.compiler]
            [reverie.system :as sys]
            [reverie.util :as util]
            [taoensso.timbre :as log]))

(defprotocol IRender
  (render [component request] [component request sub-component]))

(def get-render-fn nil)
(defmulti get-render-fn identity)
(defmethod get-render-fn :hiccup [_]
  ;; render as hiccup
  hiccup.compiler/render-html)
(defmethod get-render-fn :default [_]
  ;; if nothing is defined, we just return what we get
  identity)

(def get-data nil)
(defmulti get-data (fn [data] (::type data)))
(defmethod get-data :page/routes [data] [(assoc (:meta data) ::type :page/routes) (:data data)])
(defmethod get-data :page/no-routes [data] [(assoc (:meta data) ::type :page/no-routes) (:data data)])
(defmethod get-data :default [data] [nil data])


(def get-method-fn nil)
(defmulti ^:private get-method-fn (fn [renderer method meta] (::type meta)))
(defmethod get-method-fn :page/routes [renderer method {:keys [route-name]}]
  (assert (util/namespaced-kw? route-name) "Route was not found for renderer when rendering app/raw-page/module")
  (let [override-methods (get-in renderer [:options :override :methods-or-routes route-name])
        methods (get-in renderer [:methods-or-routes route-name])]
    (assert (not (nil? methods)) (str "No methods found for route " (util/kw->str route-name)))
    (or (get override-methods method)
        (:any override-methods)
        (get methods method)
        (:any methods))))
(defmethod get-method-fn :page/no-routes [_ _ _]
  identity)
(defmethod get-method-fn :default [renderer method _]
  (let [override-methods (get-in renderer [:options :override :methods-or-routes])
        methods (get-in renderer [:methods-or-routes])]
    (or (get override-methods method)
        (:any override-methods)
        (get methods method)
        (:any methods))))

;; Renderer takes data that has already been computed
;; and runs it through one of it's
(defrecord Renderer [name options methods-or-routes]
  IRender
  (render [this data]
    ;; render the data that's come in
    ((get-render-fn (:render-fn options)) data))
  (render [this method data]
    ;; pick out the method to render with
    (let [[meta data] (get-data data)
          method-fn (get-method-fn this method meta)
          resp (if method-fn (method-fn data) data)
          render-fn (get-render-fn (:render-fn options))]
      ;; if a method does exist, fetch the render-fn and then render against
      ;; what we get back from running the method fn with the data

      (match [(::type meta) (not (nil? method-fn)) (map? resp)]
             ;; RawPage/AppPage with a mapped method to one of the routes
             [:page/routes true true]
             (reduce (fn [out [k v]]
                       (assoc out k (render-fn v))) {} resp)

             ;; RawPage/AppPage with no mapped method to one of the routes, but with a template
             [:page/no-routes _ true]
             (reduce (fn [out [k v]]
                       (assoc out k (render-fn v))) {} data)

             ;; RawPage/AppPage with no mapped method to one of the routes, with no template
             [:page/no-routes _ _]
             (render-fn resp)

             ;; ReverieObject
             [nil true _]
             (render-fn resp)

             ;; nothing
             [_ _ _] nil))))


;; fetch the renderer to be rendered by virtue
;; of the keyword being sent in and send it off
;; to the Renderer/render combo
(extend-type clojure.lang.Keyword
  IRender
  (render [this data]
    (render (sys/renderer this) data))
  (render [this method data]
    (render (sys/renderer this) method data)))


(extend-type nil
  IRender
  (render [this request]
    (log/error "tried to render on nil")
    {:status 500
     :body "Internal Server Error"
     :headers {}})
  (render [this request sub-component]
    (log/error "tried to render on nil")
    {:status 500
     :body "Internal Server Error"
     :headers {}}))
