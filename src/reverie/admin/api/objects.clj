(ns reverie.admin.api.objects
  (:require [clj-time.core :as time]
            [korma.core :as k]
            [reverie.admin.updated :as updated]
            [reverie.auth.user :as user]
            [reverie.atoms :as atoms]
            [reverie.object :as object]
            [reverie.page :as page])
  (:use [reverie.core :only [defpage]]
        [reverie.middleware :only [wrap-access]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))


(defn- default-data [obj]
  (let [obj (keyword obj)]
   (into {} (map
             (fn [[name {:keys [initial]}]]
               {name initial}) (-> @atoms/objects obj :options :attributes)))))


(defpage "/admin/api/objects" {:middleware [[wrap-json-params]
                                            [wrap-json-response]
                                            [wrap-access :edit]]}
  [:post ["/add/:page-serial/:area/:object-name" _]
   (let [p (page/get {:serial (read-string page-serial) :version 0})
         data (default-data object-name)]
     (do
       (object/add! {:page-id (:id p) :name object-name :area area} data)
       (updated/via-page (:id p))
       {:result true}))]
  [:post ["/move/area" {:keys [object-id hit-mode anchor]}]
   (do
     (updated/via-object (read-string object-id))
     {:result (object/move! {:object-id (read-string object-id)
                             :anchor anchor
                             :hit-mode hit-mode})})]
  [:post ["/move" {:keys [object-id hit-mode]}]
   (do
     (updated/via-object (read-string object-id))
     {:result (object/move! {:object-id (read-string object-id)
                             :anchor nil
                             :hit-mode hit-mode})})]
  [:post ["/cut" {:keys [object-id]}]
   (let [object-id (read-string object-id)
         obj (object/get object-id)]
     (swap! atoms/settings assoc-in [:edits :objects object-id] {:time (time/now)
                                                                 :user (:name (user/get))
                                                                 :page-id (:page_id obj)
                                                                 :action :cut})
     {:result true
      :object (select-keys obj [:page_id :id])})]
  [:post ["/copy" {:keys [object-id]}]
   (let [object-id (read-string object-id)
         obj (object/get object-id)]
     (swap! atoms/settings assoc-in [:edits :objects object-id] {:time (time/now)
                                                                 :user (:name (user/get))
                                                                 :page-id (:page_id obj)
                                                                 :action :copy})
     {:result true
      :object (select-keys obj [:page_id :id])})]
  [:post ["/paste" {:keys [object-id area page-serial type after-object-id action]}]
   (let [u (user/get)
         object-id (read-string object-id)
         obj (get-in @atoms/settings [:edits :objects object-id])]
     (if (= (:name u) (:user obj))
       (let [object-id (case action
                         "copy" (:id (object/copy! object-id))
                         object-id)]
         (case type
           "object" (do
                      (updated/via-object object-id)
                      (swap! atoms/settings update-in [:edits :objects] dissoc object-id)
                      {:result (object/move! {:object-id object-id
                                              :hit-mode "object-paste"
                                              :anchor area
                                              :after-object-id (read-string after-object-id)})})
           "area" (let [page-serial (read-string page-serial)
                        p (page/get {:serial page-serial :version 0})]
                    (updated/via-page (:id p))
                    (swap! atoms/settings update-in [:edits :objects] dissoc object-id)
                    {:result (object/move! {:object-id object-id
                                            :page-serial page-serial
                                            :hit-mode "area-paste"
                                            :anchor area})})
           {:result false}))
       {:result false}))]
  [:post ["/app-path" {:keys [object-id app-path]}]
   (let [object-id (read-string object-id)]
     (do
       (updated/via-object object-id)
       (k/update reverie.entity/object
                 (k/set-fields {:app_path app-path})
                 (k/where {:id object-id}))
       {:result true}))]
  [:post ["/delete" {:keys [object-id]}]
   (let [object-id (read-string object-id)
         o (-> reverie.entity/object
               (k/select (k/where {:id object-id}))
               first)]
     (do
       (updated/via-object object-id)
       (k/delete (keyword (:name o)) (k/where {:object_id object-id}))
       (k/delete reverie.entity/object (k/where {:id object-id}))
       {:result true}))])
