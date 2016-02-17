(ns reverie.admin.api.interface.frames.pages
  "Namespace for manipulating the tree by destructive changes. Handled through iframes."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [cheshire.core :refer [encode]]
            [hiccup.page :refer [html5]]
            [hiccup.form :as hf]
            [reverie.admin.helpers :as helpers]
            [reverie.admin.looknfeel.common :as common]
            [reverie.admin.looknfeel.form :as form]
            [reverie.admin.validation :as validation]
            [reverie.auth :as auth]
            [reverie.cache :as cache]
            [reverie.database :as db]
            [reverie.module.entity :as e]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.publish :as publish]
            [reverie.system :as sys]
            [reverie.time :as time]
            [ring.util.anti-forgery :refer :all]
            [vlad.core :as vlad])
  (:import [reverie.admin.looknfeel.form PageForm]))


(defn get-page-form []
  (form/PageForm.
   {:fields
    {:name {:name "Name"
            :type :text
            :validation (vlad/attr [:name] (vlad/present))}
     :slug {:name "Slug"
            :type :slug
            :for :name
            :validation (vlad/attr [:slug] (vlad/present))
            :help "A slug is a part of a URL. Normally it's a normalized string of the name of the page"}
     :title {:name "Title"
             :type :text}
     :type {:name "Type of page"
            :type :dropdown
            :options [["Normal page" "page"] ["App" "app"]]}
     :template {:name "Template"
                :type :dropdown
                :options helpers/get-template-names}
     :app {:name "App"
           :type :dropdown
           :options helpers/get-app-names}}
    :sections [{:fields [:name :slug :title]}
               {:name "Meta" :fields [:template :type :app]}]}))

(defn get-meta-form []
  (form/PageForm.
   {:fields
    {:name {:name "Name"
            :type :text
            :validation (vlad/attr [:name] (vlad/present))}
     :slug {:name "Slug"
            :type :slug
            :for :name
            :validation (vlad/attr [:slug] (vlad/present))
            :help "A slug is a part of a URL. Normally it's a normalized string of the name of the page"}
     :title {:name "Title"
             :type :text}
     :type {:name "Type of page"
            :type :dropdown
            :options [["Normal page" "page"] ["App" "app"]]}
     :template {:name "Template"
                :type :dropdown
                :options helpers/get-template-names}
     :app {:name "App"
           :type :dropdown
           :options helpers/get-app-names}}
    :sections [{:fields [:name :slug :title]}
               {:name "Meta" :fields [:template :type :app]}]}))

(def ^:private cache-clear-options [["Don't clear the cache based on time" ""]
                                    ["Clear the cache every minute" 1]
                                    ["Clear the cache every 5 minutes" 5]
                                    ["Clear the cache every 10 minutes" 10]
                                    ["Clear the cache every 15 minutes" 15]
                                    ["Clear the cache every 20 minutes" 20]
                                    ["Clear the cache every 30 minutes" 30]
                                    ["Clear the cache every hour" 60]])

(defn get-cache-form []
  (form/PageForm.
   {:save-as :_save-cache
    :fields
    {:cache_cache? {:name "Cache this page?"
                    :type :boolean}
     :cache_clear_time {:name "Clear the cache based on time (for public view only)"
                        :type :dropdown
                        :options cache-clear-options
                        :help "Set this option if you wish to clear the cache based on time"}

     :cache_key_user? {:name "Cache per user?"
                       :type :boolean
                       :help "If set to true the caching system will cache the page on a per user basis in addition to the public view"}
     :cache_clear_user_time {:name "Clear the cache based on time (for logged in users)"
                             :type :dropdown
                             :options cache-clear-options
                             :help "Set this option if you wish to clear the cache based on time"}}
    :sections [{:name "Caching" :fields [:cache_cache? :cache_clear_time :cache_key_user? :cache_clear_user_time]}]}))

(defn get-menu-form []
  (form/PageForm.
   {:save-as :_save-menu
    :fields
    {:menu_hide? {:name "Hide in menu?"
                  :type :boolean
                  :help "If set to true, the page will not show up in various menus"}}
    :sections [{:name "Menu" :fields [:menu_hide?]}]}))


(defn clean-form-params [form-params]
  (walk/keywordize-keys
   (dissoc form-params
           "_method"
           "_continue"
           "_addanother"
           "_save")))

(defn process-page-form [request page-form]
  ;; get the boolean types, create a map of false values
  ;; and merge it with the cleaned params in such a manner
  ;; that the cleaned params will override the values
  ;; of the booleans map. this is for boolean values not
  ;; being sent by the browser when they're not filled in
  (let [booleans (->> (get-in page-form [:options :fields])
                      (filter (fn [[k {:keys [type]}]]
                                (= type :boolean)))
                      (map (fn [[k _]]
                             {k false}))
                      (into {}))
        form-params (merge booleans
                           (clean-form-params (:form-params request)))
        errors (validation/validate page-form form-params)]
    {:form-params form-params
     :errors errors}))

(defn add-page [request _ {:keys [parent-serial errors] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db parent-serial false)
        {:keys [form-params]} (process-page-form request (get-page-form))]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - add page")
       [:body
        [:nav
         [:div.container "Add page"]]
        [:div.container
         [:h1 "Add child to " (page/name page)]
         [:div.row.admin-interface
          (form/get-page-form (get-page-form)
                              {:form-params form-params
                               :errors errors})]
         [:footer
          (common/footer {:filter-by #{:base :editing}})]]])

      (html5
       [:head
        [:title "reverie - add page"]]
       [:body "You are not allowed to add a page here"]))))

(defn handle-add-page [request page {:keys [parent-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db parent-serial false)]
    (if (auth/authorize? page user db "edit")
      (let [{:keys [form-params
                    errors]} (process-page-form request (get-page-form))]
        (if (empty? errors)
          (let [page (db/add-page! db (assoc form-params :parent parent-serial))]
            (html5
             (common/head "reverie - add page")
             [:body
              [:nav
               [:div.container "Add page"]]
              [:div.container
               [:h1 "Page " (page/name page) " added"]]
              (str "<script type='text/javascript'>"
                   "parent.framecontrol.window.tree.add_child("
                   (json/generate-string
                    {:title (page/name page)
                     :key (page/serial page)
                     :lazy false
                     :published_p false
                     :path (page/path page)
                     :page_title (page/title page)
                     :created (time/format (page/created page) :mysql)
                     :updated (time/format (page/updated page) :mysql)})
                   ");"
                   "</script>")]))
          (add-page request page (assoc params :errors errors))))
      (html5
       [:head
        [:title "reverie - add page"]]
       [:body "You are not allowed to edit this page"]))))

(defn trash-page [request page {:keys [page-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - delete page")
       [:body
        [:nav
         [:div.container "Delete page"]]
        [:div.container
         [:h1 "Delete page " (page/name page)]
         (if (nil? (page/parent page))
           [:div.error "YOU ARE NOT ALLOWED TO DELETE THE ROOT PAGE!"]
           (list
            (hf/form-to
             ["POST" ""]
             (anti-forgery-field)
             [:table.table
              [:tr [:th "Name"] [:td (page/name page)]]
              [:tr [:th "Title"] [:td (page/title page)]]
              [:tr [:th "Created"] [:td (time/format (page/created page) :mysql)]]
              [:tr [:th "Updated"] [:td (time/format (page/updated page) :mysql)]]
              [:tr [:th]
               [:td (hf/submit-button {:class "btn btn-primary"} "Delete this page")]]])))]])
      (html5
       [:head
        [:title "reverie - delete page"]]
       [:body "You are not allowed to edit this page"]))))

(defn handle-trash-page [request page {:keys [page-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - delete page")
       [:body
        [:nav
         [:div.container "Delete page"]]
        [:div.container
         (if (nil? (page/parent page))
           [:div.error "YOU ARE NOT ALLOWED TO DELETE THE ROOT PAGE!"]
           (do (publish/trash-page! db (page/id page))
               (list [:h1 "Deleted page " (page/name page)]
                     (str "<script type='text/javascript'>"
                          "parent.framecontrol.window.tree.remove_node();"
                          "</script>"))))]])
      (html5
       [:head
        [:title "reverie - delete page"]]
       [:body "You are not allowed to edit this page"]))))

(defn meta-page [request _ {:keys [page-serial errors] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)
        [k form-used] (cond
                       (contains? params :_save-cache)
                       [:cache (get-cache-form)]

                       (contains? params :_save-menu)
                       [:menu (get-menu-form)]

                       :else
                       [:page (get-page-form)])
        {:keys [form-params]} (process-page-form
                               (update-in request [:form-params]
                                          merge
                                          (page/raw page)
                                          {:cache_cache? (get-in (page/properties page) [:cache :cache?])
                                           :cache_key_user? (get-in (page/properties page) [:cache :key :user?])
                                           :cache_clear_time (get-in (page/properties page) [:cache :clear :time])
                                           :cache_clear_user_time (get-in (page/properties page) [:cache :clear :user :time])}
                                          {:menu_hide? (get-in (page/properties page) [:menu :hide?])})
                               form-used)]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - meta")
       [:body
        [:nav
         [:div.container "Meta"]]
        [:div.container
         [:h1 "Editing " (page/name page)]
         [:div.row.admin-interface
          [:div#tabbar
           (map (fn [[id name]]
                  [:div {:class (if (= "page" id)
                                  "goog-tab goog-tab-selected"
                                  "goog-tab")
                         :tab id}
                   name])
                [["page" "Page"]
                 ["cache" "Cache"]
                 ["menu" "Menu"]])]
          [:div#page
           (form/get-page-form (get-meta-form)
                               {:form-params form-params
                                :errors errors})]
          [:div#cache.hidden
           (form/get-page-form (get-cache-form)
                               {:form-params form-params
                                :errors errors})]
          [:div#menu.hidden
           (form/get-page-form (get-menu-form)
                               {:form-params form-params
                                :errors errors})]]
         [:footer
          (common/footer {:filter-by #{:base :editing}})]]])

      (html5
       [:head
        [:title "reverie - meta"]]
       [:body "You are not allowed to edit this page"]))))

(defn handle-meta-page [request page {:keys [page-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)]
    (if (auth/authorize? page user db "edit")
      (let [[k form-used] (cond

                           (contains? params :_save-cache)
                           [:cache (get-cache-form)]

                           (contains? params :_save-menu)
                           [:menu (get-menu-form)]

                           :else
                           [:page (get-page-form)])
            {:keys [form-params errors]} (process-page-form request form-used)]
        (if (empty? errors)
          (let [page (if (= k :page)
                       (db/update-page!
                        db (page/id page)
                        (assoc form-params
                          :slug (if (nil? (page/parent page))
                                  "/"
                                  (:slug form-params))))
                       page)]
            (when (= k :cache)
              (db/save-page-properties! db (page/serial page) (dissoc form-params :_save-cache :__anti-forgery-token)))
            (when (= k :menu)
              (db/save-page-properties! db (page/serial page) (dissoc form-params :_save-menu :__anti-forgery-token)))
            (html5
             (common/head "reverie - meta")
             [:body
              [:nav
               [:div.container "Meta"]]
              [:div.container
               [:h1 "Page " (page/name page) " has been updated"]
               (if (= k :page)
                 (str "<script type='text/javascript'>"
                      "parent.framecontrol.window.tree.update_node("
                      (json/generate-string
                       {:title (page/name page)
                        :path (page/path page)
                        :slug (page/slug page)
                        :page_title (page/title page)
                        :created (time/format (page/created page) :mysql)
                        :updated (time/format (page/updated page) :mysql)})
                      ");"
                      "</script>"))]]))
          (meta-page request page (assoc params :errors errors))))
      (html5
       [:head
        [:title "reverie - meta"]]
       [:body "You are not allowed to edit this page"]))))

(defn publish-page [request page {:keys [page-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - publish page")
       [:body
        [:nav
         [:div.container "Publish page"]]
        [:div.container
         [:h1 "Publish page " (page/name page)]
         (hf/form-to
          ["POST" ""]
          (anti-forgery-field)
          [:table.table
           [:tr [:th "Name"] [:td (page/name page)]]
           [:tr [:th "Title"] [:td (page/title page)]]
           [:tr [:th "Created"] [:td (time/format (page/created page) :mysql)]]
           [:tr [:th "Updated"] [:td (time/format (page/updated page) :mysql)]]
           [:tr [:th]
            [:td
             (hf/submit-button {:class "btn btn-primary"
                                :name :__publish} "Publish this page")
             (if (page/published? page)
               (hf/submit-button {:class "btn btn-warning"
                                  :style "margin-left: 15px;"
                                  :name :__unpublish} "Unpublish this page"))]]])]])
      (html5
       [:head
        [:title "reverie - publish page"]]
       [:body "You are not allowed to edit this page"]))))

(defn handle-publish-page [request page {:keys [page-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)
        publish? (contains? params :__publish)
        unpublish? (contains? params :__unpublish)]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - un/publish page")
       [:body
        [:nav
         [:div.container "un/publish page"]]
        [:div.container
         (if publish?
           (do (publish/publish-page! db (page/id page))
               (when-let [cm (sys/get-cachemanager)]
                 (cache/evict! cm page))
              [:h1 "Published page " (page/name page)])
           (do (publish/unpublish-page! db (page/id page))
               (when-let [cm (sys/get-cachemanager)]
                 (cache/evict! cm page))
               [:h1 "Unpublished page " (page/name page)]))]])
      (html5
       [:head
        [:title "reverie - un/publish page"]]
       [:body "You are not allowed to edit this page"]))))
