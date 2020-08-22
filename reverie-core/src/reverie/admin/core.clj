(ns reverie.admin.core
  (:require [reverie.core :refer [defpage]]))

(defn empty-get [request page params]
  {})


(defpage "/admin"
  {:template :admin/index}
  [["" {:get empty-get}]])
