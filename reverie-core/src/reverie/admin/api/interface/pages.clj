(ns reverie.admin.api.interface.pages
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


(defn- get-node-data [page]
  {:title (page/name page)
   :key (page/serial page)
   :lazy (page/children? page)
   :extraClasses (str/join " "
                           (when (page/published? page)
                             "published"))

   :published_p (page/published? page)
   :path (page/path page)
   :page_title (page/title page)
   :created (time/format (page/created page) :mysql)
   :updated (time/format (page/updated page) :mysql)})

(defn get-pages [request page {:keys [id]}]
  (->>
   (let [db (:database page)]
     (let [root (db/get-page db (or id 1) false)]
       [(if (nil? id)
          (assoc (get-node-data root)
            :children (map get-node-data (page/children root))
            :lazy false
            :expanded true
            :selected true)
          (map get-node-data (page/children root)))]))
   json-response))

(defn edit-page! [request module {:keys [serial edit_p]}]
  (json-response
   (let [serial (edn/read-string serial)
         edit? (edn/read-string edit_p)
         user (get-in request [:reverie :user])
         db (:database module)
         page (db/get-page db serial false)]
     (match [(auth/authorize? page user db "edit") edit?]
            [false _] false
            [_ true] (editors/edit! page user)
            [_ false] (editors/stop-edit! user)))))

(defn update-page! [request module {:keys [serial] :as params}]
  (json-response
   (let [serial (edn/read-string serial)
         user (get-in request [:reverie :user])
         db (:database module)
         page (db/get-page db serial false)]
     (if (auth/authorize? page user db "edit")
       (try
         (db/update-page! db (page/id page) params)
         (catch Exception e
           (log/warn e)
           false))
       false))))