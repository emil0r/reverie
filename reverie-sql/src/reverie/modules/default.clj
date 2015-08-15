(ns reverie.modules.default
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ez-web.paginator :as paginator]
            [ez-web.uri :refer [join-uri]]
            [ez-web.breadcrumbs :refer [crumb]]
            ;; force the evaluation of fields in a certain order
            ;; with the help of hiccup/html. this is mostly to
            ;; get downstream to play nice
            [hiccup.core :refer [html]]
            [reverie.admin.looknfeel.common :as common]
            [reverie.admin.looknfeel.form :refer [get-entity-form
                                                  delete-entity-form]]
            [reverie.admin.validation :as validation]
            [reverie.auth :refer [with-access]]
            [reverie.database :as db]
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.system :as sys]
            [reverie.util :refer [qsize]]
            [ring.util.response :as response]))

(def base-link "admin/frame/module")
(def pagination-limit 20)

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
           "_publish"
           "_unpublish"
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

(defn skip-stages [entity stage form-params]
  (reduce (fn [out k]
            (let [field (get (e/fields entity) k)
                  skip-stages (:validation-skip-stages field)]
              (if (some #(= stage %) skip-stages)
                (assoc out k ::skip)
                (assoc out k (get form-params k)))))
          {} (keys (e/fields entity))))

(defn process-request [request module edit? stage]
  (let [{:keys [entity id]} (:params request)
        entity (m/get-entity module entity)
        id (pk-cast id)
        post-fn (or (e/post-fn entity) (fn [x & _] x))
        pre-save-fn (or (e/pre-save-fn entity) (fn [x & _] x))
        form-params (->> (post-fn (:form-params request) edit?)
                         (clean-form-params)
                         (skip-stages entity stage))
        errors (validation/validate entity form-params)]
    {:entity entity
     :id id
     :pre-save-fn pre-save-fn
     :form-params form-params
     :errors errors}))

(defn list-entities [request module params]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (array-map
     :content
     (html [:div.col-md-12
            [:table.table.entities
             [:tr
              [:th "Name"]]
             (map (fn [entity]
                    [:tr [:td [:a {:href (join-uri base-link (m/slug module) (e/slug entity))}
                               (e/name entity)]]]) (m/entities module))]])
     :nav (html (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]] {:last? true}))))))

(defn entity-does-not-exist [module]
  (array-map
   :content [:div.col-md-12 "This entity does not exist"]
   :nav
   [:ul
    [:li [:a {:href (join-uri base-link (m/slug module))}
          "Back to the start"]]]))

(defn plural? [length singular plural]
  (if (> length 1)
    (str length " " plural)
    (str length " " singular)))

(defn pagination [{:keys [uri params query-params] :as request} module entity]
  (let [qs query-params
        db (:database module)
        page (or (:page params) 1)

        {:keys [pages page
                next-seq
                prev-seq]
         :as paginated} (paginator/paginate
                         (-> (db/query db
                                       {:select [:%count.*]
                                        :from [(e/table entity)]})
                             first :count)
                         pagination-limit page)
        first 1
        last pages
        prev-seq (reverse (take 2 prev-seq))
        next-seq (take 2 next-seq)]
    (if (> pages 1)
      [:ul
       [:li.page
        [:a
         {:href (str uri "?" (qsize (assoc qs "page" first)))}
         "first"]]
       (map (fn [page]
              [:li.page
               [:a
                {:href (str uri "?" (qsize (assoc qs "page" page)))}
                (str page)]])
            prev-seq)
       [:li.page [:span (str page)]]
       (map (fn [page]
              [:li.page
               [:a
                {:href (str uri "?" (qsize (assoc qs "page" page)))}
                (str page)]])
            next-seq)
       [:li.page
        [:a
         {:href (str uri "?" (qsize (assoc qs "page" last)))}
         "last"]]])))

(defn list-entity [request module {:keys [entity page] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (if-let [entity (m/get-entity module entity)]
      (let [db (:database module)
            {:keys [order]} (:options entity)
            pk (e/pk entity)
            table (e/table entity)
            page (try (Integer/parseInt page)
                      (catch Exception _
                        1))
            display (e/display entity)
            data (db/query db {:select (into #{} (concat display [pk]))
                               :from [table]
                               :order-by [order]
                               :limit pagination-limit
                               :offset (* pagination-limit
                                          (dec page))})]
        (array-map
         :content
         (html (list
                [:div.options
                 [:ul
                  [:li [:a.btn.btn-primary
                        {:href (join-uri base-link (m/slug module) (e/slug entity) "add")}
                        (str "Add " (e/name entity))]]]]
                [:table.table.entity
                 [:tr
                  (map (fn [h]
                         [:th (name h)]) display)]
                 (map (fn [d]
                        [:tr (map (fn [k]
                                    [:td (if (= k (first display))
                                           [:a {:href (join-uri base-link (m/slug module) (e/slug entity) (str (get d pk)))} (get d k)]
                                           (get d k))]) display)])
                      data)]))
         :nav (html (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                                     [(join-uri base-link (m/slug module) (e/slug entity))  (e/name entity)]])))
         :pagination (html (pagination request module entity))))
      (entity-does-not-exist module))))

(defn single-entity [request module {:keys [entity id] :as params}
                     & [{:keys [errors published? unpublished?]}]]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (if-let [entity (m/get-entity module entity)]
      (let [id (pk-cast id)
            entity-data (m/get-data module entity params id)]
        (array-map
         :content (html
                   (get-entity-form
                    module
                    entity
                    (merge {:entity-id id
                            :errors errors
                            :display-name (get-display-name entity entity-data)
                            :published? published?
                            :unpublished? unpublished?
                            :error-field-names (when-not (empty? errors)
                                                 (e/error-field-names entity))}
                           (select-keys request [:uri])
                           entity-data)))
         :nav (html (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                                     [(e/slug entity) (e/name entity)]
                                     [(str id) (get-display-name entity entity-data)]])))
         :footer (common/footer {:filter-by #{:base :editing}})))
      (entity-does-not-exist module))))

(defn handle-single-entity [request module {:keys [entity id] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (let [{:keys [entity
                  id
                  pre-save-fn
                  form-params
                  errors]} (process-request request module true :edit)]
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

           (contains? params :_publish)
           (do (m/publish-data module entity id)
               (single-entity request module params {:published? true}))

           (contains? params :_unpublish)
           (do (m/unpublish-data module entity id)
               (single-entity request module params {:unpublished? true}))

           :else
           (single-entity request module params)))
        (single-entity request module params {:errors errors})))))

(defn add-entity [request module {:keys [entity] :as params}
                  & [{:keys [errors published?]}]]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (if-let [entity (m/get-entity module entity)]
      (let [entity-data (m/get-data module entity params)]
        (array-map
         :content (html
                   (get-entity-form
                    module
                    entity
                    (merge
                     {:errors errors
                      :published? published?
                      :display-name (get-display-name entity entity-data)
                      :error-field-names (when-not (empty? errors)
                                           (e/error-field-names entity))}
                     (select-keys request [:uri])
                     entity-data)))
         :nav (html (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                                     [(e/slug entity) (e/name entity)]
                                     ["add" "Add"]])))
         :footer (common/footer {:filter-by #{:base :editing}})))
      (entity-does-not-exist module))))

(defn handle-add-entity [request module {:keys [entity] :as params}]
  (with-access
    (get-in request [:reverie :user]) (:required-roles (m/options module))
    (let [{:keys [entity
                  pre-save-fn
                  form-params
                  errors]} (process-request request module true :add)]
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

           (contains? params :_publish)
           (do (m/publish-data module entity entity-id)
               (add-entity request module params {:published? true}))

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
        (array-map
         :content (html (delete-entity-form module entity
                                            {:entity-id id
                                             :display-name (get-display-name entity entity-data)}))
         :nav (html (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                                     [(e/slug entity) (e/name entity)]
                                     [(str id) (get (:form-params entity-data)
                                                    (first (e/display entity)))]
                                     ["delete" "Delete?"]])))))
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
