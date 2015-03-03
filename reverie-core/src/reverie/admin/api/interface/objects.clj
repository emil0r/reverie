(ns reverie.admin.api.interface.objects
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [reverie.admin.api.util :refer [boolean? json-response]]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.publish :as publish]
            [taoensso.timbre :as log]))


(defn- object-belongs? [page object-id]
  (some #(= object-id (object/id %)) (page/objects page)))

(defn delete-object! [request page {:keys [object_id page_serial]}]
  (json-response
   (let [db (get-in request [:reverie :database])
         user (get-in request [:reverie :user])
         serial (edn/read-string page_serial)
         object-id (edn/read-string object_id)
         page (db/get-page db serial false)]
     (if (and (auth/authorize? page user db "edit")
              (object-belongs? page object-id))
       (do (publish/trash-object! db object-id)
           true)
       {:success false
        :error "You do not have the rights to edit this page!"}))))


(defn add-object! [request page {:keys [page_serial area object]}]
  (json-response
   (let [db (get-in request [:reverie :database])
         user (get-in request [:reverie :user])
         serial (edn/read-string page_serial)
         page (db/get-page db serial false)]
     (if (auth/authorize? page user db "edit")
       (do (db/add-object! db {:page_id (page/id page)
                               :area area
                               :route ""
                               :name object
                               :properties {}})
           true)
       {:success false
        :error "You do not have the rights to edit this page!"}))))


(defn move-object! [request page {:keys [page_serial object_id direction ]}]
  (json-response
   (let [db (get-in request [:reverie :database])
         user (get-in request [:reverie :user])
         object-id (edn/read-string object_id)
         serial (edn/read-string page_serial)
         page (db/get-page db serial false)]
     (if (and (auth/authorize? page user db "edit")
              (object-belongs? page object-id))
       (do (db/move-object! db object-id direction)
           true)
       {:success false
        :error "You do not have the rights to edit this page!"}))))

(defn move-object-to-area! [request page {:keys [page_serial area object_id]}]
  (json-response
   (let [db (get-in request [:reverie :database])
         user (get-in request [:reverie :user])
         object-id (edn/read-string object_id)
         serial (edn/read-string page_serial)
         page (db/get-page db serial false)]
     (if (and (auth/authorize? page user db "edit")
              (object-belongs? page object-id))
       (do (db/move-object! db object-id (page/id page) area)
           true)
       {:success false
        :error "You do not have the rights to edit this page!"}))))
