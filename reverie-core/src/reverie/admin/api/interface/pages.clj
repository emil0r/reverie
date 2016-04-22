(ns reverie.admin.api.interface.pages
  "Namespace for manipulating pages through JSON. Mostly used by the user interface tree"
  (:require [clojure.core.match :refer [match]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [reverie.admin.api.editors :as editors]
            [reverie.admin.api.util :refer [boolean? json-response]]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.page :as page]
            [reverie.time :as time]
            [taoensso.timbre :as log]))


(defn- get-node-data [db page]
  {:title (page/name page)
   :key (page/serial page)
   :lazy (page/children? page db)
   :extraClasses (str/join " "
                           (when (page/published? page)
                             ["published"]))

   :published_p (page/published? page)
   :path (page/path page)
   :slug (page/slug page)
   :page_title (page/title page)
   :created (time/format (page/created page) :mysql)
   :updated (time/format (page/updated page) :mysql)})

(defn get-pages [{{db :database} :reverie :as request} page {:keys [id]}]
  (->>
   (let [root (db/get-page db (or id 1) false)]
     (if (nil? id)
       [(assoc (get-node-data db root)
               :children (map (partial get-node-data db) (page/children root db))
               :lazy false
               :expanded true
               :selected true)]
       (map (partial get-node-data db) (page/children root db))))
   json-response))

(defn edit-page! [{{db :database} :reverie :as request} module {:keys [serial edit_p]}]
  (json-response
   (let [serial (edn/read-string serial)
         edit? (edn/read-string edit_p)
         user (get-in request [:reverie :user])
         page (db/get-page db serial false)]
     (match [(auth/authorize? page user db "edit") ;; are we authorized to edit the page?
             edit? ;; do we want to edit the page?
             ]
            [false _] false
            [_ true] (editors/edit! page user)
            [_ false] (editors/stop-edit! user)))))


(defn move-page! [{{db :database} :reverie :as request} module {:keys [serial origo_serial movement] :as params}]
  (json-response
   (let [serial (edn/read-string serial)
         origo-serial (edn/read-string origo_serial)
         user (get-in request [:reverie :user])
         page (db/get-page db serial false)
         origo (db/get-page db origo-serial false)]
     (if (and (auth/authorize? page user db "edit")
              (auth/authorize? origo user db "edit"))
       (try
         (db/move-page! db (page/id page) (page/id origo) movement)
         true
         (catch Exception e
           (log/warn e)
           {:success false
            :error "You are not allowed to move this page here"}))
       {:success false
        :error "You are not allowed to move this page here"}))))
