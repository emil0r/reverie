(ns reverie.render
  "Render protocol and Renderer implementation"
  (:require [hiccup.compiler]
            [reverie.system :as sys]
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


;; Renderer takes data that has already been computed
;; and runs it through one of it's
(defrecord Renderer [name options methods]
  IRender
  (render [this data]
    ;; render the data that's come in
    ((get-render-fn (:render-fn options)) data))
  (render [this method data]
    ;; pick out the method to render with
    (let [method-fn (or (get methods method)
                        (:any methods))]
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
