(ns reverie.admin.api
  (:require [korma.core :as k]
            [reverie.admin.helpers :as helpers]
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


(defn- get-meta-options [data k options option-ks]
  (assoc-in
   data
   [k :options]
   (reduce (fn [out k]
             (if (nil? k)
               out
               (if (vector? k)
                 (let [[k default] k]
                   (assoc out k (get-in options [k] default)))
                 (assoc out k (get-in options [k])))))
           {}
           option-ks)))

(defn- get-meta-app-paths [data type app]
  (if (= type :app)
    (assoc-in data [app :paths] (helpers/get-app-paths app))
    data))

(defn- get-meta-info
  "Get meta info from templates, objects or apps. option-ks can have defaults (a vector of two: [key default])"
  [option-ks type [k v]]
  (let [k (keyword k)]
   (-> {}
       (get-meta-options k (:options v) option-ks)
       (get-meta-app-paths type k))))

(defn- get-meta []
  (let [root-serial (get-root-serial)
        u (user/get)]
    (-> {}
        (assoc :init-root-page? (init-root-page?))
        (assoc :templates (into {}  (map (partial get-meta-info [:template/areas]
                                                  :template) @atoms/templates)))
        (assoc :objects (into {} (map (partial get-meta-info []
                                               :object) @atoms/objects)))
        (assoc :apps (into {} (map (partial get-meta-info [:app/areas
                                                           [:app/type :template]]
                                            :app) @atoms/apps)))
        (assoc :edits (get-edit-actions u))
        (assoc :pages {:root root-serial
                       :current root-serial}))))

(rev/defpage "/admin/api" {:middleware [[wrap-json-params]
                                        [wrap-json-response]
                                        [wrap-access :edit]]}
  [:get ["/meta"]
   (get-meta)])
