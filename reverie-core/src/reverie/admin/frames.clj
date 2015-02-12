(ns reverie.admin.frames
  (:require [reverie.core :refer [defpage]]))


(defn- control-panel [request page params]
  {})

(defpage "/admin/frame/controlpanel" {:template :admin/control-panel}
  [["/" {:get control-panel}]])


(defpage "/admin/frame/options" {:template :admin/main}
  [["/" {:get control-panel}]])
