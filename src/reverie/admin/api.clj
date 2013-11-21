(ns reverie.admin.api
  (:require [korma.core :as k]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.util :as util])
  (:use reverie.entity
        [reverie.middleware :only [wrap-access]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))

(defn- init-root-page? []
  (=
   0
   (->
    (k/select page
              (k/aggregate (count :*) :count)
              (k/where {:parent 0 :version 0}))
    first
    :count)))

(defn- get-root-serial []
  (->  page
       (k/select
        (k/where {:parent 0 :version 0}))
       first
       :serial))

(defn- get-edit-actions [u]
  {:page (ffirst (filter (fn [[uri data]] (= (:user data) (:name u)))
                         (remove keyword?
                                 (get-in @atoms/settings [:edits]))))
   :object {:cut (ffirst (filter (fn [[object-id data]]
                                   (and
                                    (= (:user data) (:name u))
                                    (= :cut (:action data))))
                                 (get-in @atoms/settings [:edits :objects])))
            :copy (ffirst (filter (fn [[object-id data]]
                                    (and
                                     (= (:user data) (:name u))
                                     (= :copy (:action data))))
                                  (get-in @atoms/settings [:edits :objects])))}})

(defn- get-options [option-ks [k v]]
  (merge
   {:options (select-keys (:options v) option-ks)}
   {:name (util/kw->str k)}))

(defn- get-meta []
  (let [root-serial (get-root-serial)
        u (user/get)]
   {:init-root-page? (init-root-page?)
    :templates (sort-by :name (map (partial get-options [:template/areas]) @atoms/templates))
    :objects (sort-by :name (map (partial get-options []) @atoms/objects))
    :apps (sort-by :name (map (partial get-options [:app/areas]) @atoms/apps))
    :edits (get-edit-actions u)
    :pages {:root root-serial
            :current root-serial}}))

(rev/defpage "/admin/api" {:middleware [[wrap-json-params]
                                        [wrap-json-response]
                                        [wrap-access :edit]]}
  [:get ["/meta"]
   (get-meta)])
