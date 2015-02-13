(ns reverie.modules.default
  (:require [ez-web.uri :refer [join-uri]]
            [ez-web.breadcrumbs :refer [crumb]]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.system :as sys]))

(def ^:private base-link "admin/frame/module")

(defn list-entities [request module params]
  {:nav (crumb [[(join-uri base-link (m/slug module)) (m/name module)]])
   :content
   [:div.col-md-12
    [:table.striped
     [:tr
      [:th "Name"]]
     (map (fn [entity]
            [:tr [:td [:a {:href (join-uri base-link (m/slug module) (e/slug entity))}
                       (e/name entity)]]]) (m/entities module))]]})

(defn list-entity [request module {:keys [entity] :as params}]
  (if-let [entity (m/get-entity module entity)]
    {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                           [(join-uri base-link (m/slug module) (e/slug entity))  (e/name entity)]]))}
    {:nav
     [:ul
      [:li [:a {:href (join-uri base-link (m/slug module))}
            "Back to the start"]]]
     :content [:div.col-md-12 "This entity does not exist"]}))


(swap! sys/storage assoc :module-default-routes
       [["/" {:get list-entities}]
        ["/:entity" {:get list-entity}]])


;;(m/get-entity (get-in @sys/storage [:modules :auth :module]) "user")

;;(crumb [["/" "first"]])
