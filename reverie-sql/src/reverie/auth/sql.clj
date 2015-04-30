(ns reverie.auth.sql
  (:require [reverie.database :as db]
            [reverie.module :as m]
            [reverie.page :as page]
            [reverie.auth :refer [IAuthorize]]
            [reverie.util :as util])
  (:import [reverie.page Page AppPage RawPage]
           [reverie.module Module]))

(extend-type Page
  IAuthorize
  (authorize? [page user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.page/Page"]
                                         [:= :id_int (page/serial page)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [page db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.page/Page"
                               :id_int (page/serial page)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [page db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.page/Page"]
                           [:= :id_int (page/serial page)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))

(extend-type AppPage
  IAuthorize
  (authorize? [page user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.page/AppPage"]
                                         [:= :id_int (page/serial page)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [page db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.page/AppPage"
                               :id_int (page/serial page)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [page db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.page/AppPage"]
                           [:= :id_int (page/serial page)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))

(extend-type RawPage
  IAuthorize
  (authorize? [page user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.page/RawPage"]
                                         [:= :id_string (page/path page)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [page db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.page/RawPage"
                               :id_string (page/path page)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [page db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.page/RawPage"]
                           [:= :id_string (page/path page)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))

(extend-type Module
  IAuthorize
  (authorize? [module user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.module/Module"]
                                         [:= :id_string (m/name module)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [module db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.module/Module"
                               :id_string (m/name module)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [module db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.module/Module"]
                           [:= :id_string (m/name module)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))
