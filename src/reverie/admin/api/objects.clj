(ns reverie.admin.api.objects
  (:require [clj-time.core :as time]
            [korma.core :as k]
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
  [:post ["/add/:page-serial/:area/:object-name" _]
   (let [p (page/get {:serial (read-string page-serial) :version 0})
         data (default-data object-name)]
     (do
       (object/add! {:page-id (:id p) :name object-name :area area} data)
       {:result true}))]
  [:post ["/move/area" {:keys [object-id hit-mode anchor]}]
   {:result (object/move! {:object-id (read-string object-id)
                           :anchor anchor
                           :hit-mode hit-mode})}]
  [:post ["/move" {:keys [object-id hit-mode]}]
   {:result (object/move! {:object-id (read-string object-id)
                           :anchor nil
                           :hit-mode hit-mode})}]
  [:post ["/cut" {:keys [object-id]}]
   (let [object-id (read-string object-id)
         obj (object/get object-id)]
     (swap! atoms/settings assoc-in [:edits :objects object-id] {:time (time/now)
                                                                 :user (:name (user/get))
                                                                 :page-id (:page_id obj)
                                                                 :action :cut})
     {:result true
      :object (select-keys obj :page_id :id)})]
  [:post ["/copy" {:keys [object-id]}]
   (let [object-id (read-string object-id)
         obj (object/get object-id)]
     (swap! atoms/settings assoc-in [:edits :objects object-id] {:time (time/now)
                                                                 :user (:name (user/get))
                                                                 :page-id (:page_id obj)
                                                                 :action :copy})
     {:result true
      :object (select-keys obj :page_id :id)})]
  [:post ["/paste" {:keys [object-id area page-serial]}]
   (let [u (user/get)
         obj (get-in @atoms/settings [:edits :objects object-id])]
     (if (= (:name u) (:user obj))
       {:result true}
       {:result false}))]
  [:post ["/delete" {:keys [object-id]}]
   (let [o (-> reverie.entity/object
               (k/select (k/where {:id (read-string object-id)}))
               first)]
     (do
       (k/delete (keyword (:name o)) (k/where {:object_id (read-string object-id)}))
       (k/delete reverie.entity/object (k/where {:id (read-string object-id)}))
       {:result true}))])
