(ns reverie.cache.sql
  (:require [clj-time.core :as t]
            [ez-database.core :as db]
            [reverie.cache :as cache]
            [reverie.core :refer [defmodule]]
            [reverie.page :as page]
            [reverie.system :as sys]
            [reverie.time :as time]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn- evict-public? [meta]
  (nil? (:user-id meta)))

(defn- evict-private? [meta]
  (not (nil? (:user-id meta))))

(defrecord BasicStrategy []
  cache/IPruneStrategy
  (prune! [this cachemanager]
    (let [{db :database} cachemanager
          minute (Integer/parseInt (time/format (t/now) "mm"))

          ;; both public and private views will
          ;; be purged from the cache if they're
          ;; set to a blank string (i.e., don't cache)
          ;; the reason for this being that a change
          ;; in caching from caching to no caching
          ;; is not picked up for a purge
          ;; TODO: refactor this so that it's purged directly
          ;; when the meta data for the page is saved

          ;; for public view
          public-pages
          (->> (db/query db {:select [:page_serial :value]
                             :from [:reverie_page_properties]
                             :where [:= :key "cache_clear_time"]})
               (map (fn [{:keys [page_serial value]}]
                      [page_serial (if (not= value "")
                                     (Integer/parseInt value)
                                     1)])))

          ;; for private view (i.e., logged in users)
          private-pages
          (->> (db/query db {:select [:page_serial :value]
                             :from [:reverie_page_properties]
                             :where [:= :key "cache_clear_user_time"]})
               (map (fn [{:keys [page_serial value]}]
                      [page_serial (if (not= value "")
                                     (Integer/parseInt value)
                                     1)])))]
      (doseq [[serial value] public-pages]
        (when (zero? (mod minute value))
          (cache/evict! cachemanager (page/page {:serial serial}) evict-public?)))
      (doseq [[serial value] private-pages]
        (when (zero? (mod minute value))
          (cache/evict! cachemanager (page/page {:serial serial}) evict-private?))))))

(defn get-basicstrategy []
  (BasicStrategy.))

(defn- list-clear-cache-on-page [{:keys [name serial]}]
  [:div.cached-page.checkbox
   [:label
    [:input {:type :checkbox
             :name :__page
             :id serial
             :value serial}]
    name]])

(defn show-cache [request page params]
  (let [db (get-in request [:reverie :database])
        pages (->> (db/query db {:select [:p.name :p.serial]
                                 :from [[:reverie_page :p]]
                                 :join [[:reverie_page_properties :rpp]
                                        [:= :rpp.page_serial :p.serial]]
                                 :where [:and
                                         [:= :p.version 1]
                                         [:not= :rpp.value ""]
                                         [:= :rpp.key "cache_clear_time"]]}))]
    {:nav "Cache mananger"
     :content [:div.col-md-12.cache
               [:form {:method "POST" :action ""}
                (anti-forgery-field)
                [:h2 "Clear all cache"]
                [:input.btn.btn-primary.clear
                 {:type :submit
                  :name :__clear-all-cache
                  :id :__clear-all-cache
                  :value "Clear!"}]
                [:div.cached-pages
                 [:h2 "Clear pages from individual pages"]
                 (map list-clear-cache-on-page pages)
                 [:input.btn.btn-primary.clear
                  {:type :submit
                   :name :__clear-page-cache
                   :id :__clear-page-cache
                   :value "Clear!"}]]]]}))

(defn handle-cache [request page {:keys [__clear-all-cache
                                         __clear-page-cache
                                         __page] :as params}]
  (when-not (nil? __clear-all-cache)
    (when-let [cm (sys/get-cachemanager)]
      (cache/clear! cm)))
  (when-not (nil? __clear-page-cache)
    (when-let [cm (sys/get-cachemanager)]
      (let [__page (if (vector? __page)
                     __page
                     [__page])]
        (doseq [serial __page]
          (cache/evict! cm (page/page {:serial (Integer/parseInt serial)}))))))
  (show-cache request page params))

(defmodule cachemanager
  {:name "Cache manager"
   :template :admin/main}
  [["/" {:any show-cache :post handle-cache}]])
