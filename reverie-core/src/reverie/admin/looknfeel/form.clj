(ns reverie.admin.looknfeel.form
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [cheshire.core :refer [encode]]
            [ez-web.uri :refer [join-uri]]
            [hiccup.form :as form]
            [hiccup.util :refer [escape-html]]
            [reverie.admin.helpers :as helpers]
            [reverie.downstream :as downstream]
            [reverie.object :as o]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.util :as util]
            [ring.util.anti-forgery :refer :all]
            [vlad.core :as vlad]))

(defn field-name [field options]
  (or (get-in options [:name])
      (-> field name str/capitalize)))

(defn help-text [options]
  (if (:help options)
    [:div.help [:i.fa.fa-question-sign] (:help options)]))

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
                      (vlad/assign-names error-field-names)
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
    [:div.form-row (f entity field data)]
    [:div.form-row]))

(defmethod row :m2m [entity field {:keys [form-params m2m-data errors
                                          error-field-names]
                                   :or {form-params {}}}]
  (let [values (cond
                 (sequential? (get field form-params)) (get field form-params)
                 (not (nil? (get field form-params))) [(get field form-params)]
                 :else nil)
        m2m-data (get m2m-data field)
        [option-value option-name] (get-m2m-options entity field)
        options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     [:select (merge {:class :form-control}
                     {:name field :id field :multiple true}
                     (e/field-attribs entity field)
                     options)
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
                                               id module? module]
                                        :or {form-params {}}}]
  (let [inline? (e/field-attrib entity field :inline? false)]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (if inline?

       ;; inline. run the richtext editor in this field
       (let [init-tinymce-js (slurp (io/resource "public/static/admin/js/init-tinymce-inline.js"))
             format (e/field-attrib entity field :format)
             down (downstream/get :inline-admin-js [])
             filter-js (downstream/get :inline-admin-filter-js)
             script (-> init-tinymce-js
                        (str/replace  #"\|\|extra-formats\|\|"
                                      (if format
                                        (str ", " (encode {:title "Custom", :items format}))
                                        ""))
                        (str/replace #"\|selector\|" (name field)))]
         ;; tell filter-by in the common/footer function to initialize
         ;; all the js files required by :richtext
         (downstream/assoc! :inline-admin-filter-js (set/union filter-js #{:richtext}))
         ;; add the inline script to be rendered in common/footer
         (downstream/assoc! :inline-admin-js (conj down script))
         (form/text-area {:class "form-control inline-richtext"} field (form-params field)))

       ;; not inline. run the richtext editor in a popup
       (list
        [:span (merge {:field field
                       :onclick (str "window.open('/admin/api/interface/frames/richtext/" (if module? (m/slug module) id) "?field=" (util/kw->str field) "', '_blank', 'fullscreen=no, width=800, height=640, location=no, menubar=no'); return false;")}
                      (e/field-attribs entity field))
         "Edit text... "
         [:i (take 100 (escape-html (str/replace (or (form-params field) "") #"<.*?>" "")))]]
        (form/hidden-field field (form-params field))))
     (help-text (e/field-options entity field))]))

(defmethod row :boolean [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (form/check-box (merge {:class :form-control}
                            (if (:initial (e/field-options entity field))
                              {:checked true})
                            (e/field-attribs entity field)
                            options) field (form-params field))
     (help-text (e/field-options entity field))]))



(defmethod row :dropdown [entity field {:keys [form-params errors
                                               error-field-names
                                               module]
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
                       (options {:database (or (:database module)
                                               (-> entity :database))})
                       options)
                     (form-params field))
     (help-text (e/field-options entity field))]))

(defmethod row :email [entity field {:keys [form-params errors
                                            error-field-names]
                                     :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (form/email-field (merge {:class :form-control}
                              (e/field-attribs entity field)
                              options) field (form-params field))
     (help-text (e/field-options entity field))]))

(defmethod row :number [entity field {:keys [form-params errors
                                             error-field-names]
                                      :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     [:input (merge {:class :form-control}
                    (e/field-attribs entity field)
                    {:name field :id field :type :number
                     :value (form-params field)}
                    options)]
     (help-text (e/field-options entity field))]))

(defmethod row :slug [entity field {:keys [form-params errors
                                           error-field-names]
                                    :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (form/text-field (merge
                       {:class :form-control
                        :_type :slug}
                       (e/field-attribs entity field)
                       options) field (form-params field))
     (help-text (e/field-options entity field))]))

(defmethod row :textarea [entity field {:keys [form-params errors
                                               error-field-names]
                                        :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (form/text-area (merge
                      {:class :form-control}
                      (e/field-attribs entity field)
                      options) field (form-params field))
     (help-text (e/field-options entity field))]))

(defmethod row :datetime [entity field {:keys [form-params errors
                                               error-field-names]
                                        :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     [:input (merge {:type :_datetime
                     :class :form-control
                     :id field
                     :name field
                     :value (form-params field)}
                    (e/field-attribs entity field)
                    options)]
     (help-text (e/field-options entity field))]))

(defmethod row :default [entity field {:keys [form-params errors
                                              error-field-names]
                                       :or {form-params {}}}]
  (let [options (:options (e/field entity field))]
    [:div.form-row
     (error-items field errors error-field-names)
     (form/label field (e/field-name entity field))
     (form/text-field (merge
                       {:class :form-control}
                       (e/field-attribs entity field)
                       options) field (form-params field))
     (help-text (e/field-options entity field))]))



(defn get-entity-form [module entity {:keys [entity-id
                                             published?
                                             unpublished?
                                             display-name] :as data}]
  (form/form-to
   {:id :edit-form}
   ["POST" ""]
   (when published?
     [:h2 (format "Published %s: %s"
                  (str/lower-case (e/name entity))
                  display-name) ])
   (when unpublished?
     [:h2 (format "Unpublished %s: %s"
                  (str/lower-case (e/name entity))
                  display-name)])
   (anti-forgery-field)
   (map (fn [{:keys [name fields]}]
          [:fieldset
           (if name [:legend name])
           (map (fn [field]
                  (row entity field (assoc data
                                           :module? true
                                           :module module)))
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
        [:i.fa.fa-remove] "Delete"]])

    [:span.save-only.pull-right
     [:input {:type :submit :class "btn btn-primary"
              :id :_save :name :_save :value "Save"}]]
    [:span.save-continue-editing.pull-right
     [:input {:type :submit :class "btn btn-primary"
              :id :_continue :name :_continue :value "Save and continue"}]]
    [:span.save-add-new.pull-right
     [:input {:type :submit :class "btn btn-primary"
              :id :_addanother :name :_addanother :value "Save and add another"}]]
    (if (e/publishing? entity)
      (list
       (let [published?-fn (-> entity :options :publishing :published?-fn)]
         (when (and entity-id
                    published?-fn
                    (published?-fn module entity entity-id))
           [:span.unpublish.pull-right
            [:input {:type :submit :class "btn btn-cancel"
                     :id :_unpublish :name :_unpublish :value "Unpublish"}]]))
       [:span.publish.pull-right
        [:input {:type :submit :class "btn btn-secondary"
                 :id :_publish :name :_publish :value "Publish"}]]))]))


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


(defn get-object-form [database object data]
  (form/form-to
   {:id :edit-form}
   ["POST" ""]
   (anti-forgery-field)
   (map (fn [{:keys [name fields]}]
          [:fieldset
           (if name [:legend name])
           (map (fn [field]
                  (row (assoc object :database database) field data))
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
  (let [error-field-names (e/error-field-names page)
        save-as (get-in page [:options :save-as] :_save)]
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
                :id save-as :name save-as :value "Save"}]]])))
