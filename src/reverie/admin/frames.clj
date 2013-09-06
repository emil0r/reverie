(ns reverie.admin.frames
  (:require [korma.core :as k]
            [noir.validation :as v]
            [reverie.admin.templates :as t]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            reverie.entity
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [cheshire.core :only [generate-string]]
        [korma.core :only [sqlfn]]
        [hiccup core form]))


(defn- user-info [user]
  (cond
   (and (:first_name user) (:last_name user)) (str (:first_name user) " " (:last_name user))
   (:first_name user) (:first_name user)
   (:last_name user) (:last_name user)
   :else (:name user)))

(rev/defpage "/admin/frame/left" {}
  [:get ["/"] (t/frame
               {:css ["/admin/css/font-awesome.min.css"
                      "/admin/css/main.css"
                      "/admin/css/dyna-skin/ui.dynatree.css"]
                :js ["/admin/js/jquery-1.8.3.min.js"
                     "/admin/js/jquery-ui.custom.js"
                     "/admin/js/jquery.dynatree-1.2.4.js"
                     "/admin/js/main-dev.js"
                     "/admin/js/dev.js"
                     "/admin/js/eyespy.js"
                     "/admin/js/init.js"]}
               [:div.user-info "Logged in as " [:span (-> (user/get) user-info)]
                [:div.logout [:a {:href "/admin/logout"}
                              "Logout" [:i.icon-off]]]]
               [:div.tree
                (text-field :tree-search)
                [:i.icon-search]
                [:div#tree]
                [:div.icons
                 [:i.icon-refresh {:title "Refresh"}]
                 [:i.icon-plus-sign {:title "Add page"}]
                 [:i.icon-edit-sign {:title "Edit mode"}]
                 [:i.icon-eye-open.hidden {:title "View mode"}]
                 [:i.icon-trash {:title "Trash it"}]]]

               [:div.meta])])




(def frame-options-options {:css ["/admin/css/main.css"]
                            :js ["/admin/js/jquery-1.8.3.min.js"
                                 "/admin/js/main-dev.js"
                                 "/admin/js/eyespy.js"
                                 "/admin/js/init.js"]})


(defn error-item [[error]]
  [:p.error error])

(defn- table-row
  ([th td error]
     [:tr [:th th] [:td td error]])
  ([options th td error]
     [:tr options [:th th] [:td td error]]))

(defn page-form [{:keys [parent name title type template app uri]}]
  (form-to
   [:post ""]
   [:table.table.small
    (hidden-field :parent parent)
    (table-row (label :name "Name") (text-field :name name) (v/on-error :name error-item))
    (table-row (label :title "Title") (text-field :title title) nil)
    (if (pos? parent)
      (table-row (label :uri "Uri") (text-field :uri uri) (v/on-error :uri error-item)))
    (table-row (label :type "Type") (drop-down :type ["normal" "app"] type) nil)
    (table-row {:class "template"} (label :template "Template") (drop-down :template (atoms/get-templates) template) (v/on-error :template error-item))
    (table-row {:class "hidden app"} (label :app "App") (drop-down :app (atoms/get-apps) app) (v/on-error :app error-item))
    (table-row nil (submit-button {:class "btn btn-primary"} "Create") nil)]))


(defn- valid-uri? [uri]
  (nil? (re-find #"[^a-zA-Z0-9\.\-\_]" uri)))

(defn- conflicting-uri? [parent uri]
  (let [compare-uri (str (:uri (page/get {:serial parent :version 0}))
                         "/" uri)
        pages (page/get* {:parent parent :version 0})]
    (empty? (filter #(= compare-uri (:uri %)) pages))))

(defn valid-page? [{:keys [parent name type template app uri]}]
  (let [parent (read-string parent)]
    (v/rule (v/has-value? name) [:name "Name is required"])
    (if (= type "normal")
      (v/rule (v/has-value? template) [:template "You must choose a template"])
      (v/rule (v/has-value? app) [:app "You must choose an app"]))
    (if (not (zero? parent))
      (do
        (v/rule (valid-uri? uri) [:uri "The URI is not valid. Only a-zA-Z0-9.-_ is allowed."])
        (v/rule (conflicting-uri? parent uri) [:uri "The URI conflicts with another page using the same URI."])
        (v/rule (v/has-value? uri) [:uri "The URI must have a path"])))
    (not (v/errors? :name :template :app :uri))))


(rev/defpage "/admin/frame/options" {}
  [:get ["/"] nil]

  [:get ["/new-root-page"]
   (t/frame
    frame-options-options
    [:h2 "No root page exists. Please create a new one."]
    (page-form {:parent 0}))]

  [:post ["/new-root-page" {:keys [parent name title type template app uri] :as data}]
   (if (valid-page? data)
     (let [tx (page/add! {:tx-data {:uri "/" :order 0 :version 0
                                    :name name :app (or app "")
                                    :title title :parent (read-string parent)
                                    :type type :template (or template "")}})]
       
       (t/frame
        (assoc frame-options-options :custom-js
               ["parent.control.framec.reverie.admin.tree.reload();"])
        [:h2 "Root page added!"]))
     (t/frame
      frame-options-options
      [:h2 "No root page exists. Please create a new one."]
      (page-form {:parent (read-string parent) :name name :title title :type type
                  :template template :app app :uri uri})))]

  [:get ["/publish/:serial"]
   (let [p (page/get {:serial (read-string serial) :version 0})]
     (t/frame
      frame-options-options
      [:h2 "Publishing"]
      (form-to [:post ""]
               [:table.table.small
                [:tr [:th "Name"] [:td (:name p)]]
                [:tr [:th "Title"] [:td (:title p)]]
                [:tr [:th "Created"] [:td (:created p)]]
                [:tr [:th "Updated"] [:td (:updated p)]]
                [:tr [:th "Published?"] [:td (util/published? p)]]
                [:tr [:th] [:td (submit-button {:class "btn btn-primary"} "Publish")]]])))]
  [:post ["/publish/:serial" data]
   (let [p (page/get {:serial (read-string serial) :version 0})]
     (t/frame
      frame-options-options
      [:h2 "Publishing"]
      (if (or (user/admin?) (user/staff?))
        (do
          (page/publish! {:serial (read-string serial)})
          [:div.success "Published!"])
        [:div.error "You are not allowed to publish"])))]

  [:get ["/restore"]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version -1})]
     (t/frame
      frame-options-options
      [:h2 "Restore: " (:name p)]
      (form-to [:post ""]
               [:table.table.small
                [:tr [:th "Name"] [:td (:name p)]]
                [:tr [:th "Title"] [:td (:title p)]]
                [:tr [:th "Created"] [:td (:created p)]]
                [:tr [:th "Updated"] [:td (:updated p)]]
                [:tr [:th] [:td (submit-button {:class "btn btn-primary"} "Restore")]]])))]
  [:post ["/restore" data]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version -1})]
     (if (or (user/admin?) (user/staff?))
        (do
          (page/restore! {:serial (read-string serial)})
          (t/frame
           (assoc frame-options-options :custom-js
                  ["parent.control.framec.reverie.admin.tree.restored("
                   (generate-string {:serial (:serial p)
                                     :parent (:parent p)})
                   ");"])
           [:h2 "Restore: " (:name p)]
           [:div.success "Restored!"]))
        (t/frame
         frame-options-options
         [:div.error "You are not allowed to restore this page"])))]
  
  [:get ["/delete"]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version 0})]
     (t/frame
      frame-options-options
      [:h2 "Delete: " (:name p)]
      (form-to [:post ""]
               [:table.table.small
                [:tr [:th "Name"] [:td (:name p)]]
                [:tr [:th "Title"] [:td (:title p)]]
                [:tr [:th "Created"] [:td (:created p)]]
                [:tr [:th "Updated"] [:td (:updated p)]]
                [:tr [:th] [:td (submit-button {:class "btn btn-primary"} "Delete")]]])))]
  
  [:post ["/delete" data]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version 0})]
     (if (or (user/admin?) (user/staff?))
        (do
          (page/delete! {:serial (read-string serial)})
          (t/frame
           (assoc frame-options-options :custom-js
                  ["parent.control.framec.reverie.admin.tree.deleted("
                   (generate-string {:serial (:serial p)})
                   ");"])
           [:h2 "Delete: " (:name p)]
           [:div.success "Deleted!"]))
        (t/frame
         frame-options-options 
         [:div.error "You are not allowed to delete this page"])))]
  
  [:get ["/add-page"]
   (let [serial (get-in request [:params :serial])]
     (t/frame
      frame-options-options
      [:h2 "New page"]
      (page-form {:parent (read-string serial)})))]
  
  [:post ["/add-page" {:keys [parent name title type template app uri] :as data}]
   (if (valid-page? data)
     (let [p (page/get {:serial (read-string parent) :version 0})
           base-uri (:uri p)
           tx (:tx (page/add! {:parent (:id p)
                               :tx-data {:uri (util/join-uri base-uri uri)
                                         :name name :app (or app "")
                                         :title title :parent (read-string parent)
                                         :type type :template (or template "")}}))]
       (t/frame
        (assoc frame-options-options :custom-js
               ["parent.control.framec.reverie.admin.tree.added("
                (generate-string {:serial (:serial tx)
                                  :title name
                                  :real-title title
                                  :uri (:uri tx)
                                  :updated (:updated tx)
                                  :created (:created tx)
                                  :id (:id tx)
                                  :key (:serial tx)
                                  :published? false
                                  :isLazy false
                                  :order (:order tx)})
                ");"])
        [:h2 (str name " added!")]))
     (t/frame
      frame-options-options
      [:h2 "New page"]
      (page-form {:parent (read-string parent) :name name :title title :type type
                  :template template :app app :uri uri})))]

  [:get ["/meta"]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version 0})]
     (t/frame
      frame-options-options
      [:h2 "Meta: " (:name p)]
      (page-form p)))]
  
  [:post ["/meta" {:keys [parent name title type template app uri] :as data}]
   (if (valid-page? data)
     (let [serial (get-in request [:params :serial])
           p (page/get {:serial (read-string serial) :version 0})
           parent (page/get {:serial (read-string parent) :version 0})
           base-uri (:uri parent)
           tx (:tx (page/update! {:tx-data
                                  (assoc p
                                    :uri (util/join-uri base-uri uri) :order 0
                                    :name name
                                    :title title
                                    :type type
                                    :app (or app "")
                                    :template (or template "")
                                    :update (sqlfn now))}))]
       (t/frame
        (assoc frame-options-options :custom-js
               ["parent.control.framec.reverie.admin.tree.metad("
                (generate-string {:serial (:serial tx)
                                  :title name
                                  :real-title title
                                  :uri (:uri tx)
                                  :updated (:updated tx)
                                  :created (:created tx)
                                  :id (:id tx)
                                  :key (:serial tx)
                                  :published? false
                                  :isLazy false
                                  :order (:order tx)})
                ");"])
        [:h2 (str name " added!")]))
     (t/frame
      frame-options-options
      [:h2 "New page"]
      (page-form {:parent (read-string parent) :name name :title title :type type
                  :template template :app app :uri uri})))])



(defmulti row-edit (fn [_ {:keys [input]} _] input))
(defmethod row-edit :richtext [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td
    [:span {:name field-name :type :richtext} "Edit text"]
    (hidden-field field-name (or (data field-name) initial))]])
(defmethod row-edit :image [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td
    [:span {:name field-name :type :image} "Edit image"]
    (hidden-field field-name (or (data field-name) initial))]])
(defmethod row-edit :number [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td [:input {:type :number :id field-name :name field-name
                 :value (or (data field-name) initial)}]]])
(defmethod row-edit :default [field-name {:keys [initial input name]} data]
  [:tr
   [:td (label field-name name)]
   [:td (text-field field-name (or (data field-name) initial))]])

(defn- get-object-table [request attributes attr-order data]
  (form-to
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

(rev/defpage "/admin/frame/object/edit" {}
  [:get ["/"]
   (let [u (user/get)]
     (if (or (user/admin? u) (user/staff? u))
       (let [object-id (read-string (get-in request [:params :object-id]))
             [data object-name] (object/get object-id :name-object)
             attributes (object/get-attributes object-name)
             attr-order (object/get-attributes-order object-name)]
         (t/frame
          (assoc frame-options-options
            :title "Edit object")
          (get-object-table request attributes attr-order data)))
      [:div "You are not allowed to edit this object"]))]
  
  [:post ["/" form-data]
   (let [u (user/get)]
     (if (or (user/admin? u) (user/staff? u))
       (let [object-id (read-string (get-in request [:params :object-id]))
             [data object-name] (object/get object-id :name-object)
             attributes (object/get-attributes object-name)
             attr-order (object/get-attributes-order object-name)
             form-data (select-keys form-data (keys attributes))]
         (object/update! object-id (process-form-data form-data attributes))
         (t/frame
          (assoc frame-options-options
            :title "Edit object"
            :custom-js ["opener.reverie.dom.reload_main_BANG_();"
                        "window.close();"])
          (get-object-table request attributes attr-order form-data)))
       [:div "You are not allowed to edit this object"]))])
