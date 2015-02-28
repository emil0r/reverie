(ns reverie.admin.frames
  (:require [reverie.admin.frames.controlpanel :as cp]
            [reverie.admin.frames.options :as o]
            [reverie.core :refer [defpage]]))


(defn- control-panel [request page params]
  {})

(defpage "/admin/frame/controlpanel" {:template :admin/control-panel}
  [["/" {:get cp/controlpanel}]])


(defpage "/admin/frame/options" {:template :admin/main}
  [["/" {:get control-panel}]])
