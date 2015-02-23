(ns reverie.modules.default
  (:require [clojure.walk :as walk]
            [ez-web.uri :refer [join-uri]]
            [ez-web.breadcrumbs :refer [crumb]]
            [reverie.admin.looknfeel.form :refer [get-entity-form]]
            [reverie.database :as db]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.system :as sys]
            [ring.util.response :as response]))

(def ^:private base-link "admin/frame/module")

(defn- pk-cast [pk]
  (try
    (Integer/parseInt pk)
    (catch Exception e
      pk)))

(defn clean-form-params [form-data]
  (walk/keywordize-keys
   (dissoc form-data
           "_method"
           "_continue"
           "_addanother"
           "_save")))

(defn list-entities [request module params]
  {:nav (crumb [[(join-uri base-link (m/slug module)) (m/name module)]])
   :content
   [:div.col-md-12
    [:table.table.entities
     [:tr
      [:th "Name"]]
     (map (fn [entity]
            [:tr [:td [:a {:href (join-uri base-link (m/slug module) (e/slug entity))}
                       (e/name entity)]]]) (m/entities module))]]})

(defn list-entity [request module {:keys [entity] :as params}]
  (if-let [entity (m/get-entity module entity)]
    (let [db (:database module)
          {:keys [order]} (:options entity)
          pk (e/pk entity)
          table (e/table entity)
          display (e/display entity)
          data (db/query db {:select (into #{} (concat display [pk]))
                             :from [table]
                             :order-by [order]})]
      {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                             [(join-uri base-link (m/slug module) (e/slug entity))  (e/name entity)]]))
       :content
       (list
        [:div.options
         [:ul
          [:li [:a.btn.btn-primary
                {:href (join-uri base-link (m/slug module) (e/slug entity) "add")}
                "Add user"]]]]
        [:table.table.entity
         [:tr
          (map (fn [h]
                 [:th (name h)]) display)]
         (map (fn [d]
                [:tr (map (fn [k]
                            [:td (if (= k (first display))
                                   [:a {:href (join-uri base-link (m/slug module) (e/slug entity) (str (get d pk)))} (get d k)]
                                   (get d k))]) display)])
              data)])})
    {:nav
     [:ul
      [:li [:a {:href (join-uri base-link (m/slug module))}
            "Back to the start"]]]
     :content [:div.col-md-12 "This entity does not exist"]}))

(defn single-entity [request module {:keys [entity id] :as params}]
  (if-let [entity (m/get-entity module entity)]
    (let [id (pk-cast id)]
      {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                             [(e/slug entity) (e/name entity)]
                             [(str id) (str id)]]))
       :content (get-entity-form
                 module
                 entity
                 (merge {:entity-id id}
                        (select-keys request [:uri])
                        (m/get-data module entity id)))})
    {:nav
     [:ul
      [:li [:a {:href (join-uri base-link (m/slug module))}
            "Back to the start"]]]
     :content [:div.col-md-12 "This entity does not exist"]}))

(defn handle-single-entity [request module {:keys [entity id] :as params}]
  (let [entity (m/get-entity module entity)
        id (pk-cast id)
        post-fn (or (e/post-fn entity) (fn [x & _] x))
        form-params (clean-form-params (post-fn (:form-params request) true))]
    (m/save-data module entity id form-params)
    #spy/t [form-params params]
    (cond
     (contains? params :_addanother)
     (response/redirect (join-uri base-link
                                  (m/slug module)
                                  (e/slug entity)
                                  "add"))

     (contains? params :_save)
     (response/redirect (join-uri base-link
                                  (m/slug module)
                                  (e/slug entity)))

     :else
     (single-entity request module params))))

(defn add-entity [request module {:keys [entity id] :as params}])
(defn handle-add-entity [request module {:keys [entity id] :as params}])

(defn delete-entity [request module {:keys [entity id] :as params}])
(defn handle-delete-entity [request module {:keys [entity id] :as params}])


(swap! sys/storage assoc :module-default-routes
       [["/" {:get list-entities}]
        ["/:entity" {:get list-entity}]
        ["/:entity/add" {:get add-entity :post handle-add-entity}]
        ["/:entity/:id" {:get single-entity :post handle-single-entity}]
        ["/:entity/:id/delete" {:get delete-entity :post handle-delete-entity}]])
