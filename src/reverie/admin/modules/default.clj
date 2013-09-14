(ns reverie.admin.modules.default
  (:require [clojure.string :as s]
            [korma.core :as k])
  (:use [hiccup core form]
        [reverie.admin.templates :only [frame]]
        [reverie.admin.frames.common :only [frame-options]]
        reverie.admin.modules.helpers
        [reverie.atoms :only [modules]]
        reverie.batteries.breadcrumbs
        reverie.batteries.paginator
        [reverie.core :only [defmodule]]
        [reverie.util :only [join-uri]]))


(defn navbar [{:keys [uri real-uri] :as request}]
  (let [uri (last (s/split real-uri #"^/admin/frame/module"))
        {:keys [module module-name]} (get-module request)
        parts (remove s/blank? (s/split uri #"/"))
        uri-data (map
                  (fn [uri]
                    (cond
                     (re-find #"^\d+$" uri) [uri (get-instance-name
                                                  module
                                                  (second parts)
                                                  (read-string uri))]
                     :else [uri (s/capitalize uri)]))
                  parts)
        {:keys [crumbs]} (crumb uri-data {:base-uri "/admin/frame/module/"})]
    [:nav crumbs]))

(defn form-help-text [field-data]
  (if (:help field-data)
    [:div.help [:i.icon-question-sign] (:help field-data)]))

(defmulti form-row (fn [[_ data] _] (:type data)))
(defmethod form-row :html [[field data] extra]
  (let [func (:html data)]
    (func [field data] extra)))
(defmethod form-row :m2m [[field data] {:keys [form-data module entity entity-id]}]
  (let [{:keys [options selected]} (drop-down-m2m-data module entity field (read-string entity-id))]
    [:div.form-row
     (label field (get-field-name field data))
     (drop-down (merge {:multiple "multiple"}
                       (get-field-attribs data)) field options selected)
     (form-help-text data)]))
(defmethod form-row :boolean [[field data] {:keys [form-data]}]
  [:div.form-row
   (label field (get-field-name field data))
   (check-box (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :password [[field data] {:keys [form-data]}]
  [:div.form-row
   (label field (get-field-name field data))
   (password-field (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :email [[field data] {:keys [form-data]}]
  [:div.form-row
   (label field (get-field-name field data))
   (email-field (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :default [[field data] {:keys [form-data]}]
  [:div.form-row
   (label field (get-field-name field data))
   (text-field (get-field-attribs data) field (form-data field))
   (form-help-text data)])

(defn form-section [section entity data]
  (let [fields (get-fields entity (:fields section))]
    [:fieldset
     (if (:name section) [:legend (:name section)])
     (map #(form-row [% (get fields %)] data) (:fields section))]))

(defn get-form [entity {:keys [form-data entity-id real-uri] :as data}]
  (form-to
   {:id :edit-form}
   [:post ""]
   (hidden-field :which-save-button (:which-save-button form-data))
   (map #(form-section % entity data) (:sections entity))
   [:div.bottom-bar
    (if entity-id
      [:span.delete
       [:a {:href (join-uri real-uri "delete")}
        [:i.icon-remove] "Delete"]])
    [:span.save-only (submit-button {:class "btn btn-primary"
                                     :id :save-only} "Save")]
    [:span.save-continue-editing (submit-button {:class "btn btn-primary"
                                                 :id :save-continue-editing} "Save and continue editing")]
    [:span.save-add-new (submit-button {:class "btn btn-primary"
                                        :id :save-add-new} "Save and add new")]]))

(defmodule reverie-default-module {}
  [:get ["/"]
   (let [{:keys [module-name module]} (get-module request)]
     (frame
      frame-options
      [:div.holder
       [:nav "&nbsp;"]
       [:table.table.entities
        (map (fn [e]
               [:tr [:td [:a {:href (str "/admin/frame/module/"
                                         (name module-name)
                                         "/" (name e))}
                          (get-entity-name module e)]]])
             (keys (:entities module)))]]))]
  [:get ["/:entity"]
   (let [{:keys [module-name module]} (get-module request)
         display-fields (get-display-fields module entity)
         fields (get-fields module entity display-fields)
         {:keys [page]} (:params request)
         entities (get-entities module entity page)]
     (frame
      frame-options
      [:div.holder
       (navbar request)
       [:table.table.entity {:id entity}
        [:tr
         (map (fn [f]
                [:th (get-field-name module entity f)])
              display-fields)]
        (map #(get-entity-row % display-fields module-name entity) entities)]]))]

  [:get ["/:entity/:id" {:id #"\d+"}]
   (frame
    frame-options
    [:div.holder
     (navbar request)
     (let [{:keys [module-name module]} (get-module request)
           form-data (first (k/select entity (k/where {:id (read-string id)})))
           ent (get-in module [:entities (keyword entity)])]
       (get-form ent {:form-data form-data
                      :module module
                      :module-name module-name
                      :entity entity
                      :entity-id id
                      :real-uri (:real-uri request)}))])]
  )
