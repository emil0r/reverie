(ns reverie.admin.api.objects
  (:require [korma.core :as k]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.atoms :as atoms]
            [reverie.object :as object]
            [reverie.page :as page])
  (:use [reverie.middleware :only [wrap-access]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))


(defn- default-data [obj]
  (let [obj (keyword obj)]
   (into {} (map
             (fn [[name {:keys [initial]}]]
               {name initial}) (-> @atoms/objects obj :options :attributes)))))


(rev/defpage "/admin/api/objects" {:middleware [[wrap-json-params]
                                                [wrap-json-response]
                                                [wrap-access :edit]]}
  [:get ["/add/:page-serial/:area/:object-name"]
   (let [u (user/get)
         p (page/get {:serial (read-string page-serial) :version 0})
         data (default-data object-name)]
     (do
       (object/add! {:page-id (:id p) :name object-name :area area} data)
       {:result true}))]
  [:get ["/move/:anchor/:object-id/:hit-mode"]
   (let [u (user/get)
         anchor (if (re-find #"[0-9]+" anchor) (read-string anchor) anchor)]
     {:result (object/move! {:object-id (read-string object-id)
                             :anchor anchor
                             :hit-mode hit-mode})})]
  [:get ["/delete/:object-id"]
   (let [u (user/get)
         o (-> reverie.entity/object
               (k/select (k/where {:id (read-string object-id)}))
               first)]
     (do
       (k/delete (keyword (:name o)) (k/where {:object_id (read-string object-id)}))
       (k/delete reverie.entity/object (k/where {:id (read-string object-id)}))
       {:result true}))])
