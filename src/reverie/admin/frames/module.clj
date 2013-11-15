(ns reverie.admin.frames.module
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [korma.core :as k]
            [reverie.admin.templates :as t]
            [reverie.atoms :as atoms]
            [reverie.core :as rev])
  (:use [cheshire.core :only [encode]]
        [hiccup core]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.admin.modules.helpers :only [get-entity-table]]
        [reverie.atoms :only [modules]]
        [reverie.middleware :only [wrap-access]]))


(rev/defpage "/admin/frame/module" {:middleware [[wrap-access :edit]]}
  [:get ["/"]
   (t/frame
    (dissoc frame-options :js)
    [:h1 "You have to pick a module!"])]
  [:get ["/edit/richtext"]
   (let [field (-> request (get-in [:params :field]) keyword)
         module (@modules (-> request (get-in [:params :module]) keyword))
         entity (-> request (get-in [:params :entity]) keyword)
         id (-> request (get-in [:params :id]))
         field-data (if (s/blank? id)
                      ""
                      (-> entity
                          (get-entity-table module)
                          (k/select
                           (k/where {:id (read-string id)}))
                          first
                          (get field)))
         format (get-in module [:entities entity :fields field :format])]
     (let [init-tinymce-js (slurp (io/resource "public/admin/js/init.tinymce.js"))]
      (t/frame
       (-> frame-options
           (assoc :title "Edit module: Richtext")
           (assoc :js ["/admin/js/jquery-1.8.3.min.js"
                       "/admin/js/tinymce/tinymce.min.js"]))
       [:textarea {:style "width: 400px; height: 600px;"}
        field-data]
       [:div.buttons
        [:button.btn.btn-primary {:id :save} "Save"]
        [:button.btn.btn-warning {:id :cancel} "Cancel"]]
       ;; add custom formats if they are specified
       (str "<script type=\"text/javascript\">"
            (s/replace init-tinymce-js #"\|\|extra-formats\|\|"
                       (if format
                         (str ", " (encode {:title "Custom", :items format}))
                         ""))
            "</script>"))))])
