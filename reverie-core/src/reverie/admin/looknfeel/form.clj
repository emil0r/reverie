(ns reverie.admin.looknfeel.form
  (:require [clojure.string :as str]
            [ez-web.uri :refer [join-uri]]
            [hiccup.form :as form]
            [reverie.admin.helpers :as helpers]
            [reverie.object :as o]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.util :as util]
            [ring.util.anti-forgery :refer :all]
            vlad))

(defn field-name [field options]
  (or (get-in options [:name])
      (-> field name str/capitalize)))

(defn help-text [options]
  (if (:help options)
    [:div.help [:i.icon-question-sign] (:help options)]))

(defn error-item [error]
  [:div.error error])

(defn error-items [field errors error-field-names]
  (map (fn [{:keys [selector first-selector type] :as error}]
         (if (or (= selector [field])
                 (= first-selector [field]))
           (map error-item
                (flatten
                 (vals
                  (-> [error]
                      (vlad/assign-name error-field-names)
                      (vlad/translate-errors vlad/english-translation)))))))
       errors))

(defn get-m2m-options [entity field]
  (let [options (:options (e/field-options entity field))]
    (if (sequential? options)
      options
      [options options])))


(defmulti row (fn [entity field _] (get-in entity [:options :fields field :type])))

(defmethod row :html [entity field data]
  (if-let [f (:html (e/field-options entity field))]
    [:div.row-form (f entity field data)]
    [:div.row-form]))

(defmethod row :m2m [entity field {:keys [form-params m2m-data errors
                                          error-field-names]
                                   :or {form-params {}}}]
  (let [values (cond
                (sequential? (get field form-params)) (get field form-params)
                (not (nil? (get field form-params))) [(get field form-params)]
                :else nil)
        m2m-data (get m2m-data field)
        [option-value option-name] (get-m2m-options entity field)]
   [:div.form-row
    (error-items field errors error-field-names)
    (form/label field (e/field-name entity field))
    [:select (merge {:class :form-control}
                    {:name field :id field :multiple true}
                    (e/field-attribs entity field))
     (map (fn [data]
            [:option (merge
                      {:value (get data option-value)}
                      (if (some #(= (get data option-value) %)
                                (get form-params field))
                        {:selected true}
                        nil))
             (get data option-name)])
          m2m-data)]
    (help-text (e/field-options entity field))]))

(defmethod row :richtext [entity field {:keys [form-params errors
                                               error-field-names
                                               id]
                                        :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   [:span (merge {:field field
                  :onclick (str "window.open('/admin/api/interface/frames/object/richtext/" id "?field=" (util/kw->str field) "', '_blank', 'fullscreen=no, width=800, height=640, location=no, menubar=no'); return false;")}
                 (e/field-attribs entity field))
    "Edit text..."]
   (form/hidden-field field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :boolean [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/check-box (merge {:class :form-control}
                          (e/field-attribs entity field)) field (form-params field))
   (help-text (e/field-options entity field))])



(defmethod row :dropdown [entity field {:keys [form-params errors
                                               error-field-names]
                                        :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (form/drop-down (merge
                      {:class :form-control}
                      (e/field-attribs entity field))
                     field
                     (if (fn? options)
                       (options)
                       options)
                     (form-params field))
     (help-text (e/field-options entity field))]))

(defmethod row :email [entity field {:keys [form-params errors
                                            error-field-names]
                                     :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/email-field (merge {:class :form-control}
                            (e/field-attribs entity field)) field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :number [entity field {:keys [form-params errors
                                             error-field-names]
                                      :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   [:input (merge {:class :form-control}
                  (e/field-attribs entity field)
                  {:name field :id field :type :number
                   :value (form-params field)})]
   (help-text (e/field-options entity field))])

(defmethod row :slug [entity field {:keys [form-params errors
                                           error-field-names]
                                    :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/text-field (merge
                     {:class :form-control
                      :_type :slug}
                     (e/field-attribs entity field)) field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :textarea [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/text-area (merge
                    {:class :form-control}
                    (e/field-attribs entity field)) field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :default [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/text-field (merge
                     {:class :form-control}
                     (e/field-attribs entity field)) field (form-params field))
   (help-text (e/field-options entity field))])



(defn get-entity-form [module entity {:keys [entity-id] :as data}]
  (form/form-to
   {:id :edit-form}
   ["POST" ""]
   (anti-forgery-field)
   (map (fn [{:keys [name fields]}]
          [:fieldset
           (if name [:legend name])
           (map (fn [field]
                  (row entity field data))
                fields)])
        (e/sections entity))
   [:div.bottom-bar
    (when entity-id
      [:span.delete
       [:a {:href (join-uri "/admin/frame/module/"
                            (m/slug module)
                            (e/slug entity)
                            (str entity-id)
                            "delete")}
        [:i.icon-remove] "Delete"]])
    [:span.save-only.pull-right
     [:input {:type :submit :class "btn btn-primary"
              :id :_save :name :_save :value "Save"}]]
    [:span.save-continue-editing.pull-right
     [:input {:type :submit :class "btn btn-primary"
              :id :_continue :name :_continue :value "Save and continue"}]]
    [:span.save-add-new.pull-right
     [:input {:type :submit :class "btn btn-primary"
              :id :_addanother :name :_addanother :value "Save and add another"}]]]))


(defn delete-entity-form [module entity {:keys [display-name]}]
  (form/form-to
   {:id :delete-form}
   ["POST" ""]
   (anti-forgery-field)
   [:h2 "Really delete " display-name "?"]
   [:div.buttons
    [:input.btn.btn-primary {:id :_cancel :name :_cancel
                             :type :submit :value "Cancel"}]
    [:input.btn.btn-primary {:id :_delete :name :_delete
                             :type :submit :value "Delete!"}]]))


(defn get-object-form [object data]
  (form/form-to
   {:id :edit-form}
   ["POST" ""]
   (anti-forgery-field)
   (map (fn [{:keys [name fields]}]
          [:fieldset
           (if name [:legend name])
           (map (fn [field]
                  (row object field data))
                fields)])
        (e/sections object))
   [:div.bottom-bar
    [:span.save-only.pull-left
     [:input {:type :submit :class "btn btn-primary"
              :id :_save :name :_save :value "Save"}]]]))


(defrecord PageForm [options]
  e/IModuleEntity
  (fields [this] (:fields options))
  (field [this field] (get-in options [:fields field]))
  (display [this] (:display options))
  (post-fn [this] (get-in options [:post]))
  (pre-save-fn [this] (get-in options [:pre-save]))
  (field-options [this field]
    (get-in options [:fields field]))
  (field-attribs [this field]
    (let [options (get-in options [:fields field])]
      (reduce (fn [out k]
                (if (nil? out)
                  out
                  (if (k options)
                    (assoc out k (k options))
                    out)))
              {}
              [:max :min :placeholder :for])))
  (field-name [this field]
    (or (get-in options [:fields field :name])
        (-> field clojure.core/name str/capitalize)))
  (error-field-names [this]
    (into {}
          (map (fn [[k opt]]
                 [[k] (or (:name opt)
                          (-> k clojure.core/name str/capitalize))])
               (get-in options [:fields]))))
  (sections [this] (:sections options)))


(defn get-page-form [page data & extra]
  (let [error-field-names (e/error-field-names page)]
    (form/form-to
     {:id :edit-form}
     ["POST" ""]
     (anti-forgery-field)
     extra
     (map (fn [{:keys [name fields]}]
            [:fieldset
             (if name [:legend name])
             (map (fn [field]
                    (row page field (assoc data
                                      :error-field-names
                                      error-field-names)))
                  fields)])
          (e/sections page))
     [:div.bottom-bar
      [:span.save-only.pull-left
       [:input {:type :submit :class "btn btn-primary"
                :id :_save :name :_save :value "Save"}]]])))
