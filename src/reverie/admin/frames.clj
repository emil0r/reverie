(ns reverie.admin.frames
  (:require reverie.admin.frames.file-picker
            reverie.admin.frames.module
            reverie.admin.frames.object
            reverie.admin.frames.url-picker

            [clojure.set :as cset]
            [clojure.string :as s]
            [korma.core :as k]
            [noir.validation :as v]
            [reverie.admin.templates :as t]
            [reverie.admin.updated :as updated]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            reverie.entity
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.response :as r]
            [reverie.settings :as settings]
            [reverie.util :as util])
  (:use [cheshire.core :only [generate-string]]
        [korma.core :only [sqlfn]]
        [hiccup core form]
        [reverie.admin.frames.common :only [frame-options error-item]]
        [reverie.middleware :only [wrap-access]]))

(defn- exists? [x]
  (not (s/blank? x)))

(defn- user-info [user]
  (cond
   (and (exists? (:first_name user))
        (exists? (:last_name user))) (str (:first_name user)
                                          " "
                                          (:last_name user))
   (exists? (:first_name user)) (:first_name user)
   (exists? (:last_name user)) (:last_name user)
   :else (:name user)))

(rev/defpage "/admin/frame/left" {:middleware [[wrap-access :edit]]}
  [:get ["/"] (t/frame
               {:css ["/admin/css/font-awesome.min.css"
                      "/admin/css/main.css"
                      "/admin/css/dyna-skin/ui.dynatree.css"]
                :js (if (atoms/server-mode? :debug)
                      ["/admin/js/jquery-1.8.3.min.js"
                       "/admin/js/jquery-ui.custom.js"
                       "/admin/js/jquery.dynatree-1.2.4.js"
                       "/admin/js/main-dev.js"
                       "/admin/js/dev.js"
                       "/admin/js/eyespy.js"
                       "/admin/js/init.js"]
                      ["/admin/js/jquery-1.8.3.min.js"
                       "/admin/js/jquery-ui.custom.js"
                       "/admin/js/jquery.dynatree.min.js"
                       "/admin/js/main.js"])}
               [:div.user-info "Logged in as " [:span (-> (user/get) user-info)]
                [:div.logout [:a {:href "#"}
                              "Logout" [:i.icon-off]]]]
               [:div#tabbar
                [:div.goog-tab.goog-tab-selected
                 {:tab :navigation-meta} "Navigation/meta"]
                [:div.goog-tab {:tab :modules} "Modules"]]
               [:div.modules.hidden
                [:ul
                 (map (fn [m] [:li {:module m}
                              (or
                               (:name (m @atoms/modules))
                               (-> m clojure.string/capitalize))])
                      (sort (remove #(= :reverie-default-module %)
                                    (keys @atoms/modules))))]]
               [:div.navigation-meta
                [:div.tree
                 [:form#tree-search-form
                  (text-field :tree-search)
                  [:i#tree-search-icon.icon-search]]
                 [:div#tree]
                 [:div.icons
                  [:i.icon-refresh {:title "Refresh"}]
                  [:i.icon-plus-sign {:title "Add page"}]
                  [:i.icon-edit-sign {:title "Edit mode"}]
                  [:i.icon-eye-open.hidden {:title "View mode"}]
                  [:i.icon-trash {:title "Trash it"}]]]
                [:div#app-navigation.hidden
                 [:h2 "Possible app paths"]
                 [:ul]]
                [:div.meta]])])


(defn- table-row
  ([th td error]
     [:tr [:th th] [:td td error]])
  ([options th td error]
     [:tr options [:th th] [:td td error]]))

(defmulti ^:private table-row-attribute (fn [[_ {:keys [input]}]] input))
(defmethod ^:private table-row-attribute :checkbox [[key {:keys [name value]}]]
  (let [key (str "attribute-" (clojure.core/name key))]
    (table-row (label key name) (check-box key value) nil)))
(defmethod ^:private table-row-attribute :number [[key {:keys [name value]}]]
  (let [key (str "attribute-" (clojure.core/name key))]
    (table-row (label key name)
               [:input {:type :number :id key :name key :value value}]
               nil)))
(defmethod ^:private table-row-attribute :dropdown [[key {:keys [name value options]}]]
  (let [key (str "attribute-" (clojure.core/name key))
        options (if (fn? options) (options) options)]
    (table-row (label key name) (drop-down key options value) nil)))
(defmethod ^:private table-row-attribute :default [[key {:keys [name value]}]]
  (let [key (str "attribute-" (clojure.core/name key))]
    (table-row (label key name) (text-field key value) nil)))

(defn- page-form [{:keys [id serial parent name title type template app uri attributes app_template_bindings] :as p}]
  (form-to
   [:post ""]
   [:table.table.small
    (hidden-field :parent parent)
    (hidden-field :serial serial)

    ;; simple fields name, title, uri, type and template
    (table-row (label :name "Name") (text-field :name name) (v/on-error :name error-item))
    (table-row (label :title "Title") (text-field :title title) nil)
    (if (pos? parent)
      (table-row (label :uri "Uri") (text-field :uri (if uri
                                                       (util/uri-last-part uri)
                                                       "")) (v/on-error :uri error-item)))
    (table-row (label :type "Type") (drop-down :type ["normal" "app"] (if (nil? type)
                                                                        type
                                                                        (clojure.core/name type))) nil)
    
    (table-row (label :template "Template")
               (drop-down :template (atoms/get-templates) template)
               (v/on-error :template error-item))

    ;; app is tightly coupled with area mapping in the cljs code
    (table-row {:class "hidden app"}
               (label :app "App")
               [:select {:id :app :name :app}
                (map #(let [app-type (util/kw->str (settings/option-read :app % [:app/type] :template))
                            value (util/kw->str %)]
                        [:option {:value value
                                  :selected (= value app)}
                         (str value " [" app-type "]")])
                     (keys @atoms/apps))]
               (v/on-error :app error-item))
    
    ;; area mapping when an app is using a template
    (table-row {:class "hidden areas"}
               (label :areas "Area mapping")
               [:div {:id :area-mapping-holder}]
               nil)
    (hidden-field :app_template_bindings (if (nil? app_template_bindings)
                                           "{}"
                                           (if (string? app_template_bindings)
                                             app_template_bindings
                                            (pr-str app_template_bindings))))
    ;; page attributes
    (if-not (empty? attributes)
      (list
       (table-row nil [:strong "Attributes"] nil)
       (map table-row-attribute (sort attributes))))
    (table-row nil (submit-button {:class "btn btn-primary"}
                                  (if serial
                                    "Save"
                                    "Create")) nil)]))


(defn- valid-uri? [uri]
  (nil? (re-find #"[^a-zA-Z0-9\.\-\_]" uri)))

(defn- conflicting-uri?
  "Is there a conflicting URI for a new page?"
  [serial parent uri]
  ;; TODO: use util to put together the compare-uri
  (let [compare-uri (str (:uri (page/get {:serial parent :version 0}))
                         "/" uri)
        pages (if serial
                (k/select reverie.entity/page (k/where {:parent parent
                                                        :version 0
                                                        :serial [not= (read-string serial)]}))
                (page/get* {:parent parent :version 0}))]
    (empty? (filter #(= compare-uri (:uri %)) pages))))

(defn valid-page? [{:keys [serial parent name type template app uri]}]
  (let [parent (read-string parent)]
    (v/rule (v/has-value? name) [:name "Name is required"])
    (if (= type "normal")
      (v/rule (v/has-value? template) [:template "You must choose a template"])
      (v/rule (v/has-value? app) [:app "You must choose an app"]))
    (if (not (zero? parent))
      (do
        (v/rule (valid-uri? uri) [:uri "The URI is not valid. Only a-zA-Z0-9.-_ is allowed."])
        (v/rule (conflicting-uri? serial parent uri) [:uri "The URI conflicts with another page using the same URI."])
        (v/rule (v/has-value? uri) [:uri "The URI must have a path"])))
    (not (v/errors? :name :template :app :uri))))

(defn split-data [data]
  (let [ks (filter #(not (.startsWith (name %) "attribute-")) (keys data))
        attribute-ks (filter #(.startsWith (name %) "attribute-") (keys data))]
    [(select-keys data ks) (into {}
                                 (map
                                  (fn [[key data]]
                                    [(keyword (s/replace (name key) #"^attribute-" "")) data])
                                  (select-keys data attribute-ks)))]))

(defn post-process-attributes [attributes]
  (let [ks (cset/intersection
            (set (map first (filter (fn [[_ data]] (= (:type data) :boolean)) (atoms/list-page-attributes))))
            (cset/difference (set (keys (atoms/list-page-attributes))) (set (keys attributes))))]
    (merge attributes (into {}
                            (map (fn [k] [k "false"]) ks)))))


(rev/defpage "/admin/frame/options" {:middleware [[wrap-access :edit]]}
  [:get ["/"] nil]

  [:get ["/new-root-page"]
   (t/frame
    frame-options
    [:h2 "No root page exists. Please create a new one."]
    (page-form {:parent 0}))]

  [:post ["/new-root-page" {:keys [parent name title type template app uri] :as data}]
   (if (valid-page? data)
     (let [tx (page/add! {:tx-data {:uri "/" :order 0 :version 0
                                    :name name :app (or app "")
                                    :title title :parent (read-string parent)
                                    :type type :template (or template "")}})]
       
       (t/frame
        (assoc frame-options :custom-js
               ["parent.control.framec.reverie.admin.tree.reload();"])
        [:h2 "Root page added!"]))
     (t/frame
      frame-options
      [:h2 "No root page exists. Please create a new one."]
      (page-form {:parent (read-string parent) :name name :title title :type type
                  :template template :app app :uri uri})))]

  [:get ["/publish/:serial" {:middleware [[wrap-access :publish]]}]
   (let [serial (read-string serial)
         p (page/get {:serial serial :version 0})
         published? (page/published? {:serial serial})]
     (t/frame
      frame-options
      [:h2 "Publishing: " (:name p)]
      (form-to [:post ""]
               [:table.table.small
                [:tr [:th "Name"] [:td (:name p)]]
                [:tr [:th "Title"] [:td (:title p)]]
                [:tr [:th "Created"] [:td (:created p)]]
                [:tr [:th "Updated"] [:td (:updated p)]]
                [:tr [:th "Published?"] [:td published?]]
                [:tr [:th] [:td [:input {:type :submit :name :_publish :id :_publish
                                         :value "Publish" :class "btn btn-primary"}]]]
                (if published?
                    [:tr [:th] [:td [:input {:type :submit :name :_unpublish :id :_unpublish
                                             :value "Unpublish" :class "btn btn-warning"}]]])])))]
  [:post ["/publish/:serial" {:middleware [[wrap-access :publish]]} data]
   (let [serial (read-string serial)
         p (page/get {:serial serial :version 0})
         u (user/get)]
     (t/frame
      frame-options
      [:h2 "Publishing: " (:name p)]
      (cond
       (:_publish data) (do
                          (page/publish! {:serial serial})
                          [:div.success "Published!"])
       (:_unpublish data) (do
                            (page/unpublish! {:serial serial})
                            [:div.success "Unpublished!"]))))]

  [:get ["/restore"]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version -1})]
     (t/frame
      frame-options
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
          (assoc frame-options :custom-js
                 ["parent.control.framec.reverie.admin.tree.restored("
                  (generate-string {:serial (:serial p)
                                    :parent (:parent p)})
                  ");"])
          [:h2 "Restore: " (:name p)]
          [:div.success "Restored!"]))
       (t/frame
        frame-options
        [:div.error "You are not allowed to restore this page"])))]
  
  [:get ["/delete"]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version 0})]
     (t/frame
      frame-options
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
          (assoc frame-options :custom-js
                 ["parent.control.framec.reverie.admin.tree.deleted("
                  (generate-string {:serial (:serial p)})
                  ");"])
          [:h2 "Delete: " (:name p)]
          [:div.success "Deleted!"]))
       (t/frame
        frame-options 
        [:div.error "You are not allowed to delete this page"])))]
  
  [:get ["/add-page"]
   (let [serial (get-in request [:params :serial])]
     (t/frame
      frame-options
      [:h2 "New page"]
      (page-form {:parent (read-string serial)})))]
  
  [:post ["/add-page" {:keys [parent name title type template app uri] :as data}]
   (if (valid-page? data)
     (let [p (page/get {:serial (read-string parent) :version 0})
           base-uri (:uri p)
           tx (:tx (page/add! {:parent (:serial p)
                               :tx-data {:uri (util/join-uri base-uri uri)
                                         :name name :app (or app "")
                                         :title title :parent (read-string parent)
                                         :type type :template (or template "")}}))]
       (t/frame
        (assoc frame-options :custom-js
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
      frame-options
      [:h2 "New page"]
      (page-form {:parent (read-string parent) :name name :title title :type type
                  :template template :app app :uri uri})))]

  [:get ["/meta"]
   (let [serial (get-in request [:params :serial])
         p (page/get {:serial (read-string serial) :version 0})]
     (t/frame
      frame-options
      [:nav "Meta"]
      [:h2 (:name p)]
      (page-form p)))]
  
  [:post ["/meta" {:keys [parent name title type template app uri
                          app_template_bindings] :as data}]
   (let [serial (read-string (get-in request [:params :serial]))
         p (page/get {:serial serial :version 0})
         parent (page/get {:serial (read-string parent) :version 0})
         base-uri (:uri parent)]
     (if (valid-page? data)
       (let [[_ attributes] (split-data data)
             tx (:tx (page/update! {:serial serial
                                    :version 0
                                    :tx-data
                                    (->
                                     p
                                     (dissoc :attributes)
                                     (assoc
                                         :uri (util/join-uri base-uri uri) :order 0
                                         :name name
                                         :title title
                                         :type type
                                         :app (or app "")
                                         :template (or template "")
                                         :app_template_bindings app_template_bindings
                                         :updated (sqlfn now)))}))]
         ;; delete page attributes and add new ones
         (k/delete reverie.entity/page-attributes (k/where {:page_serial (:serial p)}))
         (k/insert reverie.entity/page-attributes
                   (k/values
                    (map
                     (fn [[key value]]
                       {:key (util/kw->str key)
                        :value value
                        :page_serial (:serial p)})
                     (post-process-attributes attributes))))
         ;; update 'updated' field in page
         (updated/via-page (:id p))
         (t/frame
          (assoc frame-options :custom-js
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
          [:nav "Meta"]
          [:h2 "Updated: " (:name p)]
          (page-form (page/get {:serial serial :version 0}))))
       (t/frame
        frame-options
        [:nav "Meta"]
        [:h2 (:name p)]
        (page-form {:parent (read-string (:parent data))
                    :serial serial
                    :name name :title title :type type
                    :template template :app app :uri uri}))))])
