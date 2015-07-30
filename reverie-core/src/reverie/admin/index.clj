(ns reverie.admin.index
  (:require reverie.admin.api
            reverie.admin.auth
            reverie.admin.frames
            reverie.admin.templates
            [reverie.core :refer [defpage]]))



(defn- admin-index [request page params]
  {})


(defpage "/admin" {:template :admin/index
                   :required-roles {:view #{:admin}}}
  [["/" {:get admin-index}]])
