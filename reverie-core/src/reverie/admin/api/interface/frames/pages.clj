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
            [reverie.database :as db]
            [reverie.module.entity :as e]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.publish :as publish]
            [reverie.system :as sys]
            [reverie.time :as time]
            [ring.util.anti-forgery :refer :all])
  (:import [reverie.admin.looknfeel.form PageForm]))


(defn get-page-form []
  (form/PageForm.
   {:fields
    {:name {:name "Name"
            :type :text
            :validation (vlad/present [:name])}
     :slug {:name "Slug"
            :type :slug
            :validation (vlad/present [:slug])
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
            :validation (vlad/present [:name])}
     :slug {:name "Slug"
            :type :slug
            :validation (vlad/present [:slug])
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

(defn clean-form-params [form-params]
  (walk/keywordize-keys
   (dissoc form-params
           "_method"
           "_continue"
           "_addanother"
           "_save")))

(defn process-page-form [request page-form]
  (let [form-params (clean-form-params (:form-params request))
        errors (validation/validate page-form form-params)]
    {:form-params form-params
     :errors errors}))

(defn add-page [request _ {:keys [parent-serial errors] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db parent-serial false)
        {:keys [form-params]} (process-add-page request (get-page-form))]
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
          (common/footer {:filter-by #{:base}})]]])

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
                    errors]} (process-add-page request (get-page-form))]
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
        {:keys [form-params]} (process-page-form
                               (update-in request [:form-params] merge (page/raw page))
                               (get-page-form))]
    (if (auth/authorize? page user db "edit")
      (html5
       (common/head "reverie - meta")
       [:body
        [:nav
         [:div.container "Meta"]]
        [:div.container
         [:h1 "Editing " (page/name page)]
         [:div.row.admin-interface
          (form/get-page-form (get-meta-form)
                              {:form-params form-params
                               :errors errors})]
         [:footer
          (common/footer {:filter-by #{:base}})]]])

      (html5
       [:head
        [:title "reverie - meta"]]
       [:body "You are not allowed to edit this page"]))))

(defn handle-meta-page [request page {:keys [page-serial] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        page (db/get-page db page-serial false)]
    (if (auth/authorize? page user db "edit")
      (let [{:keys [form-params
                    errors]} (process-page-form request (get-page-form))]
        (if (empty? errors)
          (let [page (db/update-page! db (page/id page) form-params)]
            (html5
             (common/head "reverie - meta")
             [:body
              [:nav
               [:div.container "Meta"]]
              [:div.container
               [:h1 "Page " (page/name page) " has been updated"]
               (str "<script type='text/javascript'>"
                    "parent.framecontrol.window.tree.update_node("
                    (json/generate-string
                     {:title (page/name page)
                      :path (page/path page)
                      :page_title (page/title page)
                      :created (time/format (page/created page) :mysql)
                      :updated (time/format (page/updated page) :mysql)})
                    ");"
                    "</script>")]]))
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
            [:td (hf/submit-button {:class "btn btn-primary"} "Publish this page")]]])]])
      (html5
       [:head
        [:title "reverie - publish page"]]
       [:body "You are not allowed to edit this page"]))))

(defn handle-publish-page [request page {:keys [page-serial] :as params}]
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
         (do (publish/publish-page! db (page/id page))
           [:h1 "Published page " (page/name page)])]])
      (html5
       [:head
        [:title "reverie - publish page"]]
       [:body "You are not allowed to edit this page"]))))
