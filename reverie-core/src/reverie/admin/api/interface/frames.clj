(ns reverie.admin.api.interface.frames
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [cheshire.core :refer [encode]]
            [hiccup.page :refer [html5]]
            [reverie.admin.looknfeel.common :as common]
            [reverie.admin.looknfeel.form :as form]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.system :as sys]
            [reverie.time :as time]))


(def cast-field nil)
(defmulti cast-field (fn [options & _] (match [(:type options) (:cast options)]
                                             [_ :number] :number
                                             [_ _] (:type options))))
(defmethod cast-field :number [_ value]
  (if (str/blank? value)
    nil
    (try (Integer/parseInt value)
         (catch Exception _
           (Double/parseDouble value)))))
(defmethod cast-field :datetime [_ value]
  (if (str/blank? value)
    nil
    (time/coerce value "YYYY-MM-DD HH:mm" :java.sql.Timestamp)))
(defmethod cast-field :boolean [_ value]
  (if (str/blank? value)
    false
    true))
(defmethod cast-field :default [_ value]
  value)

(defn clean-params [object params]
  (let [fields (e/fields object)]
    (reduce (fn [out [k options]]
              (assoc out k (cast-field options (params k))))
            params fields)))

(defn edit-object [{{db :database user :user} :reverie :as request} page {:keys [object-id]}]
  (let [object (db/get-object db object-id)
        data {:id object-id
              :form-params
              (merge (object/initial-fields (object/name object) {:database db})
                     (object/properties object)
                     (walk/keywordize-keys (:form-params request)))}]
    (if (and (auth/authorize? (object/page object) user db "edit")
             (zero? (page/version (object/page object))))
      (html5
       (common/head "Object editing")
       [:body
        (form/get-object-form db object data)
        (when (= (:request-method request) :post)
          [:script {:type "text/javascript"}
           "opener.dom.reload_main();"
           "window.close();"])
        (common/footer {:filter-by #{:base :editing}})])
      (html5
       [:head
        [:title "Object editing"]]
       [:body "You are not allowed to edit this object"]))))

(defn handle-object [{{db :database user :user} :reverie :as request} page {:keys [object-id] :as params}]
  (let [object (db/get-object db object-id)]
    (if (and (auth/authorize? (object/page object) user db "edit")
             (zero? (page/version (object/page object))))
      (do (db/update-object! db object-id (clean-params object params))
          (edit-object request page params))
      (html5
       [:head
        [:title "Object editing"]]
       [:body "You are not allowed to edit this object"]))))

(defn richtext [{{db :database user :user} :reverie :as request} page {:keys [field object-id module-p]}]
  (if (str/blank? module-p)
    ;; opened by an object
    (let [object (db/get-object db object-id)
          format (:format (e/field object (keyword field)))
          init-tinymce-js (slurp (io/resource "public/static/admin/js/init-tinymce.js"))]
      (if (auth/authorize? (object/page object) user db "edit")
        (html5
         (common/head "Object: Richtext")
         [:body
          [:textarea {:style "width: 400px; height: 600px;"}
           (get (object/properties object) (keyword field))]
          [:div.buttons
           [:button.btn.btn-primary {:id :save} "Save"]
           [:button.btn.btn-warning {:id :cancel} "Cancel"]]
          (common/footer {:filter-by #{:base :richtext}})
          (str "<script type='text/javascript'>"
               (str/replace init-tinymce-js #"\|\|extra-formats\|\|"
                            (if format
                              (str ", " (encode {:title "Custom", :items format}))
                              ""))
               "</script>")
          (when (= (:request-method request) :post)
            [:script {:type "text/javascript"}
             "opener.dom.reload_main();"
             "window.close();"])])
        (html5
         [:head
          [:title "Object editing"]]
         [:body "You are not allowed to edit this object"])))

    ;; opened by a module
    (let [{db :database user :user} (get-in request [:reverie])
          module (->> module-p
                      keyword
                      sys/module
                      :module)
          format (:format (m/get-entity module field))
          init-tinymce-js (slurp (io/resource "public/static/admin/js/init-tinymce.js"))]
      (if (auth/authorize? module user db "edit")
        (html5
         (common/head "Module: Richtext")
         [:body
          [:textarea {:id "textarea" :style "width: 400px; height: 600px;"}]
          [:div.buttons
           [:button.btn.btn-primary {:id :save} "Save"]
           [:button.btn.btn-warning {:id :cancel} "Cancel"]]
          (common/footer {:filter-by #{:base :richtext}})
          (str "<script type='text/javascript'>"
               (str/replace init-tinymce-js #"\|\|extra-formats\|\|"
                            (if format
                              (str ", " (encode {:title "Custom", :items format}))
                              ""))
               "document.getElementById('textarea').innerText = opener.document.getElementById('" field "').value;"
               "</script>")
          (when (= (:request-method request) :post)
            [:script {:type "text/javascript"}
             "window.close();"])])
        (html5
         [:head
          [:title "Module editing"]]
         [:body "You are not allowed to edit this module"])))))

(defn url-picker [request page params]
  (html5
   (common/head "URL:s")
   [:body
    "url picker"]))
