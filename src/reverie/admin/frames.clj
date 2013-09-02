(ns reverie.admin.frames
  (:require [noir.validation :as v]
            [reverie.admin.templates :as t]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
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
