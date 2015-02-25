(ns reverie.modules.default
  (:require [clojure.walk :as walk]
            [ez-web.uri :refer [join-uri]]
            [ez-web.breadcrumbs :refer [crumb]]
            [reverie.admin.looknfeel.form :refer [get-entity-form
                                                  delete-entity-form]]
            [reverie.admin.validation :as validation]
            [reverie.database :as db]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.security :refer [with-access]]
            [reverie.system :as sys]
            [ring.util.response :as response]))

(def base-link "admin/frame/module")

(defn pk-cast [pk]
  (try
    (Integer/parseInt pk)
    (catch Exception e
      pk)))

(defn get-display-name [entity entity-data]
  (get (:form-params entity-data)
       (first (e/display entity))))

(defn clean-form-params [form-params]
  (walk/keywordize-keys
   (dissoc form-params
           "_method"
           "_continue"
           "_addanother"
           "_save")))

(defn select-errors [errors pickings]
  (let [pickings (map (fn [error] [error]) pickings)]
   (reduce (fn [out error]
             (if (some #(= (or (:selector error)
                               (:first-selector error)) %)
                       pickings)
               (conj out error)
               out))
           [] errors)))

(defn process-request [request module edit?]
  (let [{:keys [entity id]} (:params request)
        entity (m/get-entity module entity)
        id (pk-cast id)
        post-fn (or (e/post-fn entity) (fn [x & _] x))
        pre-save-fn (or (e/pre-save-fn entity) (fn [x & _] x))
        form-params (clean-form-params (post-fn (:form-params request) edit?))
        errors (validation/validate entity form-params)]
    {:entity entity
     :id id
     :pre-save-fn pre-save-fn
     :form-params form-params
     :errors errors}))

(defn list-entities [request module params]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]] {:last? true}))
     :content
     [:div.col-md-12
      [:table.table.entities
       [:tr
        [:th "Name"]]
       (map (fn [entity]
              [:tr [:td [:a {:href (join-uri base-link (m/slug module) (e/slug entity))}
                         (e/name entity)]]]) (m/entities module))]]}))


(defn entity-does-not-exist [module]
  {:nav
   [:ul
    [:li [:a {:href (join-uri base-link (m/slug module))}
          "Back to the start"]]]
   :content [:div.col-md-12 "This entity does not exist"]})

(defn list-entity [request module {:keys [entity] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
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
      (entity-does-not-exist module))))

(defn single-entity [request module {:keys [entity id] :as params}
                     & [{:keys [errors]}]]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (if-let [entity (m/get-entity module entity)]
      (let [id (pk-cast id)
            entity-data (m/get-data module entity params id)]
        {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                               [(e/slug entity) (e/name entity)]
                               [(str id) (get-display-name entity entity-data)]]))
         :content (get-entity-form
                   module
                   entity
                   (merge {:entity-id id
                           :errors errors
                           :error-field-names (when-not (empty? errors)
                                                (e/error-field-names entity))}
                          (select-keys request [:uri])
                          entity-data))})
      (entity-does-not-exist module))))

(defn handle-single-entity [request module {:keys [entity id] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (let [{:keys [entity
                  id
                  pre-save-fn
                  form-params
                  errors]} (process-request request module true)]
      (if (empty? errors)
        (do
          (m/save-data module entity id (pre-save-fn form-params true))
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
           (single-entity request module params)))
        (single-entity request module params {:errors errors})))))

(defn add-entity [request module {:keys [entity] :as params}
                  & [{:keys [errors]}]]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (if-let [entity (m/get-entity module entity)]
      (let [entity-data (m/get-data module entity params)]
        {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                               [(e/slug entity) (e/name entity)]
                               ["add" "Add"]]))
         :content (get-entity-form
                   module
                   entity
                   (merge
                    {:errors errors
                     :error-field-names (when-not (empty? errors)
                                          (e/error-field-names entity))}
                    (select-keys request [:uri])
                    entity-data))})
      (entity-does-not-exist module))))

(defn handle-add-entity [request module {:keys [entity] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (let [{:keys [entity
                  pre-save-fn
                  form-params
                  errors]} (process-request request module true)]
      (if (empty? errors)
        (let [entity-id (m/add-data module entity (pre-save-fn form-params false))]
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
           (response/redirect (join-uri base-link
                                        (m/slug module)
                                        (e/slug entity)
                                        (str entity-id)))))
        (add-entity request module params {:errors errors})))))

(defn delete-entity [request module {:keys [entity id] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (if-let [entity (m/get-entity module entity)]
      (let [id (pk-cast id)
            entity-data (m/get-data module entity params id)]
        {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                               [(e/slug entity) (e/name entity)]
                               [(str id) (get (:form-params entity-data)
                                              (first (e/display entity)))]
                               ["delete" "Delete?"]]))
         :content (delete-entity-form module entity
                                      {:entity-id id
                                       :display-name (get-display-name entity entity-data)})})
      (entity-does-not-exist module))))

(defn handle-delete-entity [request module {:keys [entity id] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (let [entity (m/get-entity module entity)
          id (pk-cast id)]

      (cond
       (contains? params :_delete)
       (do
         (m/delete-data module entity id true)
         (response/redirect (join-uri base-link
                                      (m/slug module)
                                      (e/slug entity))))
       :else
       (response/redirect (join-uri base-link
                                    (m/slug module)
                                    (e/slug entity)
                                    (str id)))))))


(swap! sys/storage assoc :module-default-routes
       [["/" {:get list-entities}]
        ["/:entity" {:get list-entity}]
        ["/:entity/add" {:get add-entity :post handle-add-entity}]
        ["/:entity/:id" {:get single-entity :post handle-single-entity}]
        ["/:entity/:id/delete" {:get delete-entity :post handle-delete-entity}]])
