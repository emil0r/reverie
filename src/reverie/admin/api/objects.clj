(ns reverie.admin.api.objects
  (:require [korma.core :as k]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.atoms :as atoms]
            [reverie.object :as object]
            [reverie.page :as page])
  (:use [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))


(defn- default-data [obj]
  (let [obj (keyword obj)]
   (into {} (map
             (fn [[name {:keys [initial]}]]
               {name initial}) (-> @atoms/objects obj :options :attributes)))))


(rev/defpage "/admin/api/objects" {:middleware [[wrap-json-params]
                                                [wrap-json-response]]}
  [:get ["/add/:page-serial/:area/:object-name"]
   (let [u (user/get)
         p (page/get {:serial (read-string page-serial) :version 0})
         data (default-data object-name)]
     (if (or (user/admin? u) (user/staff? u))
       (do
         (object/add! {:page-id (:id p) :name object-name :area area} data)
         {:result true})))])
