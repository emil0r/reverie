(ns reverie.admin.frames
  (:require [korma.core :as k]
            [noir.validation :as v]
            [reverie.admin.templates :as t]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.page :as page]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [cheshire.core :only [generate-string]]
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
                 [:i.icon-refresh]
                 [:i.icon-plus-sign]
                 [:i.icon-edit-sign]
                 [:i.icon-eye-open.hidden]
                 [:i.icon-trash]]]

               [:div.meta
                "my meta stuff"])])




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
    (if (nil? parent)
      (table-row (label :uri "Uri") (text-field :uri uri) (v/on-error :uri error-item)))
    (table-row (label :type "Type") (drop-down :type ["normal" "app"] type) nil)
    (table-row {:class "template"} (label :template "Template") (drop-down :template (atoms/get-templates) template) (v/on-error :template error-item))
    (table-row {:class "hidden app"} (label :app "App") (drop-down :app (atoms/get-apps) app) (v/on-error :app error-item))
    (table-row nil (submit-button {:class "btn btn-primary"} "Create") nil)]))


(defn- valid-uri? [uri]
  (not (nil? (re-find #"[^a-zA-Z0-9\.\-\_]" uri))))

(defn valid-page? [{:keys [parent name type template app uri]}]
  (v/rule (v/has-value? name) [:name "Name is required"])
  (if (= type "normal")
    (v/rule (v/has-value? template) [:template "You must choose a template"])
    (v/rule (v/has-value? app) [:app "You must choose an app"]))
  (if (not (= parent "0"))
    (do
      (v/rule (valid-uri? uri) [:uri "The URI is not valid. Only a-zA-Z0-9.-_ is allowed."])
      (v/rule (v/has-value? uri) [:uri "The URI must have a path"])))
  (not (v/errors? :name :template :app :uri)))


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
      (page-form {:parent parent :name name :title title :type type
                  :template template :app app :uri uri})))]

  [:get ["/publish/:serial"]
   (let [p (page/get {:serial (read-string serial) :version 0})]
    (t/frame
     frame-options-options
     [:h2 "Publishing"]
     [:table.table.small
      [:tr [:th "Name"] [:td (:name p)]]
      [:tr [:th "Title"] [:td (:title p)]]
      [:tr [:th "Created"] [:td (:created p)]]
      [:tr [:th "Updated"] [:td (:updated p)]]
      [:tr [:th "Published?"] [:td (util/published? p)]]
      [:tr [:th] [:td (submit-button {:class "btn btn-primary"} "Publish")]]]))])
