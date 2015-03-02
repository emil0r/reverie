(ns reverie.admin.api.interface.objects
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [reverie.admin.api.util :refer [boolean? json-response]]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.page :as page]
            [reverie.publish :as publish]
            [taoensso.timbre :as log]))



(defn delete-object! [request page {:keys [object_id page_serial]}]
  (json-response
   (let [db (get-in request [:reverie :database])
         user (get-in request [:reverie :user])
         serial (edn/read-string page_serial)
         object-id (edn/read-string object_id)
         page (db/get-page db serial false)]
     #spy/t [serial object-id]
     (if (auth/authorize? page user db "edit")
       (do (publish/trash-object! db object-id)
           true)
       {:success false
        :error "You do not have the rights to edit this page!"}))))
