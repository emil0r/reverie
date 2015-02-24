(ns reverie.admin.looknfeel.form
  (:require [clojure.string :as str]
            [ez-web.uri :refer [join-uri]]
            [hiccup.form :as form]
            [reverie.module :as m]
            [reverie.module.entity :as e]
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
    [:select (merge {:name field :id field :multiple true}
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

(defmethod row :boolean [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/check-box (e/field-attribs entity field) field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :password [entity field {:keys [form-params errors
                                               error-field-names]
                                        :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/password-field (e/field-attribs entity field) field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :email [entity field {:keys [form-params errors
                                            error-field-names]
                                     :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/email-field (e/field-attribs entity field) field (form-params field))
   (help-text (e/field-options entity field))])

(defmethod row :number [entity field {:keys [form-params errors
                                             error-field-names]
                                      :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   [:input (merge (e/field-attribs entity field)
                  {:name field :id field :type :number
                   :value (form-params field)})]
   (help-text (e/field-options entity field))])

(defmethod row :default [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  [:div.form-row
   (error-items field errors error-field-names)
   (form/label field (e/field-name entity field))
   (form/text-field (e/field-attribs entity field) field (form-params field))
   (help-text (e/field-options entity field))])



(defn get-entity-form [module entity {:keys [entity-id] :as data}]
  (form/form-to
   {:id :edit-form}
   ["POST" ""]
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
    [:span.save-only
     [:input {:type :submit :class "btn btn-primary"
              :id :_save :name :_save :value "Save"}]]
    [:span.save-continue-editing
     [:input {:type :submit :class "btn btn-primary"
              :id :_continue :name :_continue :value "Save and continue"}]]
    [:span.save-add-new
     [:input {:type :submit :class "btn btn-primary"
              :id :_addanother :name :_addanother :value "Save and add another"}]]]))


(defn delete-entity-form [module entity {:keys [display-name]}]
  (form/form-to
   {:id :delete-form}
   ["POST" ""]
   [:h2 "Really delete " display-name "?"]
   [:div.buttons
    [:input.btn.btn-primary {:id :_cancel :name :_cancel
                             :type :submit :value "Cancel"}]
    [:input.btn.btn-primary {:id :_delete :name :_delete
                             :type :submit :value "Delete!"}]]))
