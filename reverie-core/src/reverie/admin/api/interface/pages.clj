(ns reverie.admin.api.interface.pages
  (:require [cheshire.core :as json]
            [reverie.database :as db]
            [reverie.page :as page]))

(defn json-response [body]
  {:status 200
   :headers {"Content-Type" "json/application"}
   :body body})

(defn- get-node-data [page]
  {:title (page/name page)
   :key (page/serial page)
   :lazy (page/children? page)
   :path (page/path page)})

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
   json/generate-string
   json-response))


;; (defn get-pages [request page params]
;;   (json-response (json/generate-string
;;                   [{:title "root" :key 1
;;                     :extraClasses "published"
;;                     :children [{:title "Child 1" :key 2}
;;                                {:title "Child 2" :key 3
;;                                 :children (map (fn [x]
;;                                                  {:title (str "Title " x)
;;                                                   :key x}) (range 50 100))}
;;                                {:title "Child 3" :key 4
;;                                 :children [{:title "Sub-child 1" :key 5}
;;                                            {:title "Sub-child 2" :key 6}]}]}])))
