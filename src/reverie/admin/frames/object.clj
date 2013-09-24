(ns reverie.admin.frames.object
  (:require [noir.validation :as v]
            [reverie.admin.templates :as t]
            [reverie.core :as rev]
            reverie.entity
            [reverie.object :as object]
            [reverie.response :as r]
            [reverie.util :as util])
  (:use [hiccup core form]
        [reverie.admin.frames.common :only [frame-options error-item]]
        [reverie.middleware :only [wrap-access]]))


(defmulti row-edit (fn [_ {:keys [input]} _] input))
(defmethod row-edit :richtext [field-name {:keys [initial input name]} data]
  (let [data (or (data field-name) initial)]
    [:tr
     [:td (label field-name name)]
     [:td
      [:span {:field-name field-name :type :richtext}
       "Edit text..."]
      (hidden-field field-name data)]]))
(defmethod row-edit :image [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td
    [:span {:field-name field-name :type :image} "Edit image..."]
    (hidden-field field-name (or (data field-name) initial))]])
(defmethod row-edit :dropdown [field-name {:keys [initial input name options]} data]
  (let [options (if (fn? options) (options) options)]
   [:tr
    [:td (label field-name name)]
    [:td (drop-down field-name options (or (data field-name) initial))
     (v/on-error field-name error-item)]]))
(defmethod row-edit :number [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td [:input {:type :number :id field-name :name field-name
                 :value (or (data field-name) initial)}]
    (v/on-error field-name error-item)]])
(defmethod row-edit :textarea [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td (text-area field-name (or (data field-name) initial))]])
(defmethod row-edit :default [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td (text-field field-name (or (data field-name) initial))]])

(defn- get-object-table [request attributes attr-order data]
  (form-to {:name :form_object}
           [:post ""]
           [:table.table
            (reduce (fn [out k]
                      (if (nil? k)
                        out
                        (conj out (row-edit k (attributes k) data))))
                    (list)
                    (reverse attr-order))
            [:tr [:td] [:td (submit-button "Save")]]]))

(defn- process-form-data [data attributes]
  (reduce (fn [out k]
            (if (nil? k)
              out
              (case (-> k attributes :input)
                :number (assoc out k (read-string (data k)))
                out)))
          data
          (keys attributes)))

(defn- valid-form-data? [attributes form-data]
  (doseq [[name {:keys [input]}] attributes]
    (case input
      :number (v/rule (v/valid-number? (form-data name)) [name "Only numbers are allowed"])
      (v/rule true [name "Should not appear"])))
  (not (apply v/errors? (keys attributes))))

(rev/defpage "/admin/frame/object/edit" {:middleware [[wrap-access :edit]]}
  [:get ["/"]
   (let [object-id (read-string (get-in request [:params :object-id]))
         [data object-name] (object/get object-id :name-object)
         attributes (object/get-attributes object-name)
         attr-order (object/get-attributes-order object-name)]
     (t/frame
      (assoc frame-options
        :title "Edit object")
      (get-object-table request attributes attr-order data)))]
  
  [:post ["/" form-data]
   (let [object-id (read-string (get-in request [:params :object-id]))
         [data object-name] (object/get object-id :name-object)
         attributes (object/get-attributes object-name)
         attr-order (object/get-attributes-order object-name)
         form-data (select-keys form-data (keys attributes))
         validated? (valid-form-data? attributes form-data)
         custom-js (if validated?
                     ["opener.reverie.dom.reload_main_BANG_();"
                      "window.close();"]
                     [])]
     (if validated?
       (object/update! object-id (process-form-data form-data attributes)))
     (t/frame
      (assoc frame-options
        :title "Edit object"
        :custom-js custom-js)
      (get-object-table request attributes attr-order form-data)))]

  [:get ["/richtext"]
   (let [field (-> request (get-in [:params :field]) keyword)
         field-data (-> request (get-in [:params :object-id]) read-string object/get field)]
     (t/frame
      (-> frame-options
          (assoc :title "Edit object: Richtext")
          (assoc :js ["/admin/js/jquery-1.8.3.min.js"
                      "/admin/js/tinymce/tinymce.min.js"
                      "/admin/js/init.tinymce.js"]))
      [:textarea {:style "width: 400px; height: 600px;"}
       field-data]
      [:div.buttons
       [:button.btn.btn-primary {:id :save} "Save"]
       [:button.btn.btn-warning {:id :cancel} "Cancel"]]))])


