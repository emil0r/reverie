(ns reverie.page
  (:require [korma.core :as k]))


;; (extend-type ReverieDataDatomic
;;   reverie-page

;;   (page-render [{:keys [connection request] :as rdata}]
;;     (let [{:keys [uri]} request]
;;       (if-let [[route-uri page-data] (get-route uri)]
;;         (let [page (rev/page-get (assoc rdata :page-id (:page-id page-data)))]
;;           (case (:type page-data)
;;             :normal (let [template (get @templates (:reverie.page/template page))
;;                           fn (:fn template)]
;;                       (fn (assoc rdata :page-id (:db/id page))))
;;             :page (let [request (shorten-uri request route-uri)
;;                         [_ route _ func] (->> route-uri
;;                                               (get @rev/pages)
;;                                               :fns
;;                                               (filter #(let [[method route _ _] %]
;;                                                          (and
;;                                                           (= (:request-method request) method)
;;                                                           (clout/route-matches route request))))
;;                                               first)]
;;                     (if (nil? func)
;;                       {:status 404 :body "404, page not found"}
;;                       (func rdata (clout/route-matches route request))))
;;             (rev/app-render (assoc rdata :page-data page-data :page page)))))))

;;   (page-objects [{:keys [connection page-id area] :as rdata}]
;;     (let [page (d/entity (db connection) page-id)]
;;       (sort-by :reverie/order
;;                (filter #(and
;;                          (:reverie/active? %)
;;                          (= (:reverie/area %) area)) (:reverie.page/objects page)))))
  
;;   (page-get-meta [rdata])
  
;;   (page-new-object! [{:keys [connection object-id page-id] :as rdata}]
;;     (let [page (d/entity (db connection) page-id)
;;           tx @(d/transact connection
;;                           [{:db/id page-id
;;                             :reverie.page/objects object-id}])]
;;       (assoc rdata :tx tx)))
  
;;   (page-update-object! [rdata]) ;; datomic allows upside travseral?

;;   (page-delete-object! [{:keys [connection object-id] :as rdata}]
;;     (let [tx @(d/transact connection
;;                           [{:db/id object-id
;;                             :reverie/active? false}])]
;;       (assoc rdata :tx tx)))

;;   (page-new! [{:keys [connection parent tx-data page-type] :as rdata}]
;;     (let [uri (:reverie.page/uri tx-data)
;;           tx @(d/transact connection
;;                           [(merge tx-data
;;                                   {:db/id #db/id [:db.part/reverie]
;;                                    :reverie/active? true
;;                                    :reverie.page/objects []})])
;;           page-id (-> tx :tempids vals last)]
;;       (add-route! uri {:page-id page-id :type (or page-type :normal)
;;                        :template (:reverie.page/template tx-data)})
;;       (merge rdata {:tx tx :page-id page-id})))

;;   (page-update! [{:keys [connection page-id tx-data] :as rdata}]
;;     (let [old-uri (:reverie.page/uri (rev/page-get rdata))
;;           new-uri (:reverie.page/uri tx-data)
;;           tx @(d/transact connection
;;                           [(merge tx-data {:db/id page-id})])]
;;       (if (and
;;            (not (nil? new-uri))
;;            (not= new-uri old-uri))
;;         (rev/update-route! new-uri (rev/get-route old-uri)))
;;       (assoc rdata :tx tx)))

;;   (page-delete! [{:keys [connection page-id] :as rdata}]
;;     (let [uri (:page.reverie/uri (rev/page-get rdata))
;;           tx @(d/transact connection
;;                           [{:db/id page-id
;;                             :reverie/active? false}])]
;;       (rev/remove-route! uri)
;;       (assoc rdata :tx tx)))

;;   (page-restore! [{:keys [connection page-id] :as rdata}]
;;     (let [tx @(d/transact connection
;;                           [{:db/id page-id
;;                             :reverie/active? true}])]
;;       (assoc rdata :tx tx)))

;;   (page-get [{:keys [connection page-id] :as rdata}]
;;     (d/entity (db connection) page-id))

;;   (page-publish! [rdata])

;;   (page-unpublish! [rdata])

;;   (page-rights? [rdata user right]))
