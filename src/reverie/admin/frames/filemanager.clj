(ns reverie.admin.frames.filemanager
  (:require [reverie.admin.templates :as t]
            [reverie.core :as rev]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.middleware :only [wrap-access]]))


(rev/defpage "/admin/frame/filemanager" {:middleware [[wrap-access :edit]]}
  [:get ["/images"]
   (t/frame
    (-> frame-options
        (assoc :title "Filemanager: Images"))
    [:div "Filemanager: Images"])])
