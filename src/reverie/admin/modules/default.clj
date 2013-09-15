(ns reverie.admin.modules.default
  (:require [clojure.string :as s]
            [noir.validation :as v]
            [korma.core :as k])
  (:use [hiccup core form]
        [reverie.admin.templates :only [frame]]
        [reverie.admin.frames.common :only [frame-options error-item]]
        reverie.admin.modules.helpers
        [reverie.atoms :only [modules]]
        reverie.batteries.breadcrumbs
        reverie.batteries.paginator
        [reverie.core :only [defmodule raise-response]]
        [reverie.middleware :only [wrap-access]]
        [reverie.responses :only [response-302]]
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

(defn- valid-oform-data? [form-data fields]
  (doseq [[field {:keys [type validation]}] fields]
    (if validation
      (doseq [[v? error-msg] validation]
        (v/rule (v? (form-data field)) [field error-msg])))
    (case type
      :number (v/rule (v/valid-number? (form-data field)) [field "Only numbers are allowed"])
      (v/rule true [field "Should not appear"])))
  (not (apply v/errors? (keys fields))))

(defn- process-form-data [data fields]
  (reduce (fn [out k]
            (if (nil? k)
              out
              (case (-> k fields :type)
                :number (assoc out k (read-string (data k)))
                :text (cond
                       (nil? (data k)) (assoc out k "")
                       :else out)
                :email (cond
                        (nil? (data k)) (assoc out k "")
                        :else out)
                :boolean (cond
                          (= (data k) "true") (assoc out k true)
                          :else (assoc out k false))
                :m2m (cond
                      (nil? (data k)) (assoc out k [])
                      (sequential? (data k)) (assoc out k (vec (map
                                                                #(if (re-find #"^\d+$" %)
                                                                   (read-string %)
                                                                   %) (data k))))
                      (re-find #"^\d+$" (data k)) (assoc out k [(read-string (data k))])
                      :else out)
                out)))
          data
          (keys fields)))

(defmulti form-row (fn [[_ data] _] (:type data)))
(defmethod form-row :html [[field data] extra]
  (let [f (:html data)]
    (f [field data] extra)))
(defmethod form-row :m2m [[field data] {:keys [form-data module entity entity-id]}]
  (let [{:keys [options selected]} (drop-down-m2m-data module entity field (read-string entity-id))]
    [:div.form-row
     (v/on-error field error-item)
     (label field (get-field-name field data))
     (drop-down (merge {:multiple "multiple"}
                       (get-field-attribs data)) field options selected)
     (form-help-text data)]))
(defmethod form-row :boolean [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   (check-box (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :password [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   (password-field (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :email [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   (email-field (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :default [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
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
   (map #(form-section % entity data) (:sections entity))
   [:div.bottom-bar
    (if entity-id
      [:span.delete
       [:a {:href (join-uri real-uri "delete")}
        [:i.icon-remove] "Delete"]])
    [:span.save-only
     [:input {:type :submit :class "btn btn-primary"
              :id :_save :name :_save :value "Save"}]]
    [:span.save-continue-editing
     [:input {:type :submit :class "btn btn-primary"
              :id :_continue :name :_continue :value "Save and continue"}]]
    [:span.save-add-new
     [:input {:type :submit :class "btn btn-primary"
              :id :_addanother :name :_addanother :value "Save and add another"}]]]))

(defmodule reverie-default-module {:middleware [[wrap-access :edit]]}
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
           form-data (-> entity
                         (k/select (k/where {:id (read-string id)}))
                         first
                         (pre-process-data module entity))
           ent (get-in module [:entities (keyword entity)])]
       (get-form ent {:form-data form-data
                      :module module
                      :module-name module-name
                      :entity entity
                      :entity-id id
                      :real-uri (:real-uri request)}))])]
  
  [:post ["/:entity/:id" {:id #"\d+"} wrong-form-data]
   (frame
    frame-options
    (let [{:keys [module module-name]} (get-module request)
          form-data (-> request
                        :form-params
                        clojure.walk/keywordize-keys
                        (dissoc :_save :_continue :_addanother))
          proceed (cond
                   (get (request :form-params) "_continue") :continue
                   (get (request :form-params) "_addanother") :add-another
                   :else :save)
          ent (get-in module [:entities (keyword entity)])
          form-data (process-form-data form-data (:fields ent))
          m2m-fields (map first
                          (filter
                           (field-type? :m2m)
                           (get-in module [:entities (keyword entity) :fields])))]
      (if (valid-form-data? form-data (:fields ent))
        (do
          (save-m2m-data module entity (read-string id) form-data)
          (k/update entity
                    (k/set-fields (into
                                   {}
                                   (remove
                                    (fn [[k _]]
                                      (some #(= k %) m2m-fields))
                                    (post-process-data form-data
                                                       module
                                                       entity))))
                    (k/where {:id (read-string id)}))
          (case proceed
            :continue (raise-response (response-302 (:real-uri request)))
            :add-another (raise-response (response-302 (join-uri "/admin/frame/module/"
                                                                 (name module-name)
                                                                 entity
                                                                 "add")))
            :save (raise-response (response-302 (join-uri "/admin/frame/module/"
                                                          (name module-name)
                                                          entity)))))
        [:div.holder
         (navbar request)
         (get-form ent {:form-data form-data
                        :module module
                        :module-name module-name
                        :entity entity
                        :entity-id id
                        :real-uri (:real-uri request)})])))]
  )
