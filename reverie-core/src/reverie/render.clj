(ns reverie.render
  (:require [taoensso.timbre :as log]))

(defprotocol IRender
  (render [component request] [component request sub-component]))


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
