(ns reverie.admin.modules.default
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [clojure.string :as s]
            [noir.validation :as v]
            [korma.core :as k])
  (:use [hiccup core form]
        [reverie.admin.frames.common :only [frame-options error-item]]
        reverie.admin.modules.helpers
        [reverie.admin.templates :only [frame]]
        [reverie.atoms :only [modules]]
        [reverie.core :only [defmodule raise-response]]
        [reverie.middleware :only [wrap-access]]
        [reverie.response :only [response-302]]
        [reverie.util :only [join-uri]]))

(defn- string->timestamp [timestamp]
  (let [timestamp (s/trim timestamp)
        timestamp (case (count timestamp)
                    16 (str timestamp ":00")
                    13 (str timestamp ":00:00")
                    10 (str timestamp "00:00:00")
                    timestamp)
        fmt (format/formatters :mysql)]
    (->> timestamp (format/parse fmt) coerce/to-sql-date)))

(defn- get-form-data [request]
  (-> request
      :form-params
      clojure.walk/keywordize-keys
      (dissoc :_save :_continue :_addanother)))

(defn- get-proceed [request]
  (cond
   (get (request :form-params) "_continue") :continue
   (get (request :form-params) "_addanother") :add-another
   :else :save))

(defn- valid-form-data? [form-data fields]
  (doseq [[field {:keys [type validation]}] fields]
    (if validation
      (doseq [[v? error-msg send-form-data?] validation]
        (if send-form-data?
          (v/rule (v? form-data) [field error-msg])
          (v/rule (v? (form-data field)) [field error-msg]))))
    (case type
      :number (v/rule (v/valid-number? (form-data field)) [field "Only numbers are allowed"])
      (v/rule true [field "Should not appear"])))
  (not (apply v/errors? (keys fields))))

(defn- process-form-data [form-data fields]
  (reduce (fn [out k]
            (if (nil? k)
              out
              (case (-> k fields :type)
                :number (assoc out k (read-string (form-data k)))
                :text (cond
                       (nil? (form-data k)) (assoc out k "")
                       :else out)
                :email (cond
                        (nil? (form-data k)) (assoc out k "")
                        :else out)
                :boolean (cond
                          (= (form-data k) "true") (assoc out k true)
                          :else (assoc out k false))
                :o2m (cond
                      (nil? (form-data k)) (assoc out k nil)
                      (s/blank? (form-data k)) (assoc out k nil)
                      :else (assoc out k (read-string (form-data k))))
                :datetime (cond
                           (nil? (form-data k)) (assoc out k nil)
                           (s/blank? (form-data k)) (assoc out k nil)
                           :else (assoc out k (string->timestamp (form-data k))))
                :m2m (cond
                      (nil? (form-data k)) (assoc out k [])
                      (sequential? (form-data k)) (assoc out k (vec (map
                                                                #(if (re-find #"^\d+$" %)
                                                                   (read-string %)
                                                                   %) (form-data k))))
                      (re-find #"^\d+$" (form-data k)) (assoc out k [(read-string (form-data k))])
                      :else out)
                out)))
          form-data
          (keys fields)))

(defmulti form-row (fn [[_ data] _] (:type data)))
(defmethod form-row :html [[field data] extra]
  (let [f (:html data)]
    (f [field data] extra)))
(defmethod form-row :m2m [[field data] {:keys [form-data module entity entity-id]}]
  (let [{:keys [options selected]} (drop-down-m2m-data module entity field form-data entity-id)]
    [:div.form-row
     (v/on-error field error-item)
     (label field (get-field-name field data))
     (drop-down (merge {:multiple "multiple"}
                       (get-field-attribs data)) field options selected)
     (form-help-text data)]))
(defmethod form-row :o2m [[field data] {:keys [form-data module entity entity-id]}]
  (let [{:keys [options selected]} (drop-down-o2m-data module entity field form-data entity-id)
        options (if (:null? data)
                  (vec (cons ["- Nothing selected -" ""] options))
                  options)]
    [:div.form-row
     (v/on-error field error-item)
     (label field (get-field-name field data))
     (drop-down (get-field-attribs data) field options selected)
     (form-help-text data)]))
(defmethod form-row :boolean [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   (check-box (get-field-attribs data) field (form-data field))
   (form-help-text data)])
(defmethod form-row :image [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   (hidden-field field (form-data field))
   [:span {:field-name field :type :image} (if (data field)
                                             (str "Image: " (data field))
                                             "Edit image...")]
   (form-help-text data)])
(defmethod form-row :richtext [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   (hidden-field field (form-data field))
   [:span {:field-name field :type :richtext} "Edit text..."]
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
(defmethod form-row :number [[field data] {:keys [form-data]}]
  [:div.form-row
   (v/on-error field error-item)
   (label field (get-field-name field data))
   [:input (merge (get-field-attribs data)
                  {:name field :id field :type :number
                   :value (form-data field)})]
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
     (if (:help section) [:div.section.help [:i.icon-question-sign] (:help section)])
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
      [:div.admin-interface
       [:nav "&nbsp;"]
       [:table.table.entities
        (map (fn [e]
               [:tr [:td [:a {:href (str "/admin/frame/module/"
                                         (name module-name)
                                         "/" (name e))}
                          (get-entity-name module e)]]])
             (sort (keys (:entities module))))]]))]
  
  [:get ["/:entity"]
   (let [{:keys [module-name module]} (get-module request)
         display-fields (get-display-fields module entity)
         fields (get-fields module entity display-fields)
         {:keys [page]} (:params request)
         entities (get-entities module entity page)]
     (frame
      frame-options
      [:div.admin-interface
       (navbar request)
       [:div.options
        [:ul
         [:li [:a.btn.btn-primary {:href (frame-join-uri module-name entity "add")}
               (str "Add " (get-entity-name module entity))]]]]
       [:table.table.entity {:id entity}
        [:tr
         (map (fn [f]
                [:th (get-field-name module entity f)])
              display-fields)]
        (map #(get-entity-row % display-fields module-name entity) entities)]
       (pagination (-> (get-entity-table entity module)
                       (k/select (k/aggregate (count :*) :count))
                       first
                       :count)
                   50 page (:real-uri request))]))]

  [:get ["/:entity/:id" {:id #"\d+"}]
   (frame
    frame-options
    [:div.admin-interface
     (navbar request)
     (let [{:keys [module-name module]} (get-module request)
           form-data (-> entity
                         (get-entity-table module)
                         (k/select (k/where {:id (read-string id)}))
                         first
                         (pre-process-data :edit module entity))
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
          form-data (get-form-data request)
          proceed (get-proceed request)
          ent (get-in module [:entities (keyword entity)])
          form-data (process-form-data form-data (:fields ent))
          m2m-fields (map first
                          (filter
                           (field-type? :m2m)
                           (get-in module [:entities (keyword entity) :fields])))]
      (if (valid-form-data? form-data (:fields ent))
        (do
          (save-m2m-data module entity (read-string id) form-data)
          (k/update (get-entity-table entity module)
                    (k/set-fields (into
                                   {}
                                   (remove
                                    (fn [[k _]]
                                      (some #(= k %) m2m-fields))
                                    (post-process-data form-data
                                                       :edit
                                                       module
                                                       entity))))
                    (k/where {:id (read-string id)}))
          (case proceed
            :continue (raise-response (response-302 (:real-uri request)))
            :add-another (raise-response (response-302 (frame-join-uri module-name
                                                                       entity
                                                                       "add")))
            :save (raise-response (response-302 (frame-join-uri module-name
                                                                entity)))))
        [:div.admin-interface
         (navbar request)
         (get-form ent {:form-data form-data
                        :module module
                        :module-name module-name
                        :entity entity
                        :entity-id id
                        :real-uri (:real-uri request)})])))]

  [:get ["/:entity/:id/delete" {:id #"\d+"}]
   (frame
    frame-options
    [:div.admin-interface
     (navbar request)
     (let [{:keys [module-name module]} (get-module request)
           ent-data (-> entity
                        (get-entity-table module)
                        (k/select (k/where {:id (read-string id)}))
                        first)]
       (form-to
        {:id :delete-form}
        [:post ""]
        [:h2 "Really delete " "mumbo jumbo" "?"]
        [:div.buttons
         [:input {:type :submit :class "btn btn-primary"
                  :id :_cancel :name :_cancel :value "Cancel"}]
         [:input {:type :submit :class "btn btn-danger"
                  :id :_delete :name :_delete :value "Delete"}]]))])]

  [:post ["/:entity/:id/delete" {:id #"\d+"} form-data]
   (frame
    frame-options
    (let [{:keys [module-name module]} (get-module request)]
      (if (:_delete form-data)
        (do
          (-> entity
              (get-entity-table module)
              (k/delete (k/where {:id (read-string id)})))
          (raise-response (response-302 (frame-join-uri module-name
                                                        entity))))
        (raise-response (response-302 (frame-join-uri module-name
                                                      entity
                                                      id))))))]
  
  [:get ["/:entity/add"]
   (frame
    frame-options
    [:div.admin-interface
     (navbar request)
     (let [{:keys [module-name module]} (get-module request)
           form-data (pre-process-data (get-entity-default-data entity
                                                                module)
                                       :add module entity)
           ent (get-in module [:entities (keyword entity)])]
       (get-form ent {:form-data form-data
                      :module module
                      :module-name module-name
                      :entity entity
                      :real-uri (:real-uri request)}))])]

  [:post ["/:entity/add" wrong-form-data]
   (frame
    frame-options
    (let [{:keys [module module-name]} (get-module request)
          form-data (get-form-data request)
          proceed (get-proceed request)
          ent (get-in module [:entities (keyword entity)])
          form-data (process-form-data form-data (:fields ent))
          m2m-fields (map first
                          (filter
                           (field-type? :m2m)
                           (get-in module [:entities (keyword entity) :fields])))]

      (if (valid-form-data? form-data (:fields ent))
        (let [{:keys [id]} (k/insert
                            (get-entity-table entity module)
                            (k/values (into
                                       {}
                                       (remove
                                        (fn [[k _]]
                                          (some #(= k %) m2m-fields))
                                        (post-process-data form-data
                                                           :add
                                                           module
                                                           entity)))))]
          
          (save-m2m-data module entity id form-data)
          (case proceed
            :continue (raise-response (response-302 (frame-join-uri module-name
                                                                    entity
                                                                    (str id))))
            :add-another (raise-response (response-302 (frame-join-uri module-name
                                                                       entity
                                                                       "add")))
            :save (raise-response (response-302 (frame-join-uri module-name
                                                                entity)))))
        [:div.admin-interface
         (navbar request)
         (get-form ent {:form-data form-data
                        :module module
                        :module-name module-name
                        :entity entity
                        :real-uri (:real-uri request)})])))]
  )
