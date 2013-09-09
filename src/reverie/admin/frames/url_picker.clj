(ns reverie.admin.frames.url-picker
  (:require [reverie.admin.templates :as t]
            [reverie.core :as rev]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.middleware :only [wrap-access]]))

(rev/defpage "/admin/frame/url-picker" {:middleware [[wrap-access :edit]]}
  [:get ["/"]
   (t/frame
    (-> frame-options
        (assoc :title "URL picker"))
    [:div "URL picker, not implemented yet."])])
