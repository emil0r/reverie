(ns reverie.render
  "Render protocol and Renderer implementation"
  (:require [hiccup.compiler]
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
(defmethod get-data ::type-app [data] [(:meta data) (:data data)])
(defmethod get-data :default [data] [nil data])


(def get-method-fn nil)
(defmulti ^:private get-method-fn (fn [methods-or-routes method meta] (nil? meta)))
(defmethod get-method-fn false [routes method {:keys [route-name]}]
  (assert (util/namespaced-kw? route-name) "Route was not found for renderer when rendering app/raw-page/module")
  (let [methods (get routes route-name)]
    (assert (not (nil? methods)) (str "No methods found for route " (util/kw->str route-name)))
    (or (get methods method)
        (:any methods))))
(defmethod get-method-fn :default [methods method _] (or (get methods method)
                                                         (:any methods)))

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
          method-fn (get-method-fn methods-or-routes method meta)]
      ;; if a method does exist, fetch the render-fn and then render against
      ;; what we get back from running the method fn with the data
      (if method-fn
        ((get-render-fn (:render-fn options)) (method-fn data))))))


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
