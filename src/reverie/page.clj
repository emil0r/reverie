(ns reverie.page
  (:refer-clojure :exclude [get meta])
  (:require [clojure.string :as s]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.app :as app]
            [reverie.page.helpers :as helpers]
            [reverie.response :as r]
            [reverie.util :as util])
  (:use [reverie.atoms :exclude [objects]]
        reverie.entity))

(defn- template->str [tx]
  (if (:template tx)
    (assoc tx :template (-> tx :template util/kw->str) )
    tx))
(defn- template->kw [tx]
  (if (:template tx)
    (assoc tx :template (-> tx :template keyword))
    tx))

(defn- clean-save-data [data]
  (dissoc data :id :serial))

(defn- get-serial-page []
  (let [serial (-> page (k/select (k/aggregate (max :serial) :serial)) first :serial)]
    (if serial
      (+ 1 serial)
      1)))

(defn get* [w]
  (k/select page (k/where w) (k/order :order)))

(defn children
  "Get the children of a page including page attributes. Accepts uri/serial of parent AND version"
  [{:keys [uri serial version]}]
  (let [children (if (nil? serial)
                   (k/select page
                             (k/where {:parent [= (k/subselect page
                                                               (k/fields :serial)
                                                               (k/where {:uri uri
                                                                         :version version}))]
                                       :version version})
                             (k/order :order))
                   (k/select page
                            (k/where {:parent serial :version version})
                            (k/order :order)))
        attributes (k/select page-attributes (k/where
                                              {:page_serial [in (map :serial children)]}))]
    (map (fn [c]
           (assoc c :attributes (helpers/transform-attributes
                                 (filter #(= (:page_serial %) (:serial c))
                                         attributes)))) children)))

(defn get
  "Get a page. serial overrides page-id. Leaving version nil gives you the unpublished version."
  [{:keys [page-id serial version]}]
  (let [version (or version 0)
        page (if (and serial version)
               (-> page (k/select
                         (k/where {:serial serial :version version}))
                   first util/revmap->kw)
               (-> page (k/select
                         (k/where {:id page-id}))
                   first util/revmap->kw))
        attributes (k/select page-attributes (k/where {:page_serial (:serial page)}))]
    (assoc page :attributes (helpers/transform-attributes attributes))))

(defn attributes? [data attributes]
  (= attributes (select-keys data (keys attributes))))

(defn- get-last-order [request]
  (let [parent (or (:parent request) (:serial (get request)))]
    (+ 1
       (or
        (-> page (k/select (k/aggregate (max :order) :order)
                           (k/where {:parent parent})) first :order)
        0))))

(defn objects
  "Get objects associated with a page. page-id required"
  [request]
  (let [{{area :area page-id :page-id app-path :app/path} :reverie} request
        w {:page_id page-id :area (util/kw->str area)}]
    (k/select object
              (k/where (and w
                            {:app_paths [in (remove nil? ["" "*" (if app-path
                                                                   (name app-path))])]}))
              (k/order :order))))

(defn render
  "Renders a page"
  [{:keys [uri] :as request}]
  (if-let [[route-uri page-data] (get-route uri)]
    (case (:type page-data)
      :normal (let [page (get {:serial (:serial page-data)
                               :version (util/which-version? request)})
                    template-options (-> page get-template :options)
                    template (clojure.core/get @templates (-> page :template keyword))
                    f (:fn template)]
                (util/middleware-wrap
                 (util/middleware-merge template-options)
                 f request))
      :page (let [request (util/shorten-uri
                           request
                           route-uri)
                  page-options (->> route-uri (clojure.core/get @pages) :options)
                  [_ route options f] (->> route-uri
                                           (clojure.core/get @pages)
                                           :fns
                                           (filter
                                            #(let [[method route _ _] %]
                                               (and
                                                (= (:request-method request) method)
                                                (clout/route-matches route
                                                                     request))))
                                           first)]
              (if (nil? f)
                (r/response-404)
                (if (= :get (:request-method request))
                  (util/middleware-wrap
                   (util/middleware-merge page-options options)
                   f request (clout/route-matches route request))
                  
                  (util/middleware-wrap
                   (util/middleware-merge page-options options)
                   f request (clout/route-matches route request) (:params request)))))
      :app (let [page (get-in request [:reverie :page])]
             (app/render (-> request
                             (assoc-in [:reverie :app] (keyword (:app page))))))
      (r/response-404))))

(defn meta [{:keys [page-id] :as request}]
  (k/select page-attributes (k/where {:page_id page-id})))

(defn add! [{:keys [tx-data] :as request}]
  (let [uri (:uri tx-data)
        type (or (:type tx-data) :normal)
        tx-data (util/revmap->str (assoc tx-data
                                    :serial (or (:serial tx-data)
                                                (get-serial-page))
                                    :order (or (:order tx-data)
                                               (get-last-order request))
                                    :version (or (:version tx-data) 0)
                                    :updated (or (:updated tx-data) (k/sqlfn now))
                                    :type type))
        tx (k/insert page (k/values (template->str tx-data)))]
    (add-route! uri {:page-id (:id tx) :type (keyword type)
                     :serial (:serial tx)
                     :template (:template tx-data) :published? false})
    (assoc request :page-id (:id tx) :tx tx)))

(defn update!
  "Update a page with the tx-data. Also updates control structures"
  [{:keys [tx-data] :as request}]
  (let [p (get request)
        old-uri (:uri p)
        new-uri (:uri tx-data)
        tx-data (util/revmap->kw tx-data)
        route-data (-> old-uri
                       get-route
                       second
                       (assoc :type (:type tx-data))
                       (assoc :template (:template tx-data))
                       (assoc :app (:app tx-data)))
        tx-data (clean-save-data (util/revmap->str tx-data))
        result (k/update page
                         (k/set-fields tx-data)
                         (k/where {:id (:id p)}))]
    (if (and
         (not (nil? new-uri))
         (not= new-uri old-uri))
      (update-route! new-uri route-data)
      (replace-route-data! old-uri route-data))
    {:tx result}))

(defn delete! [{:keys [serial]}]
  (k/delete page
            (k/where {:serial serial :version [> 0]}))
  (k/update page
            (k/set-fields {:order 0 :version -1})
            (k/where {:serial serial :version 0})))

(defn restore! [{:keys [serial]}]
  (k/update page
            (k/set-fields {:order (get-last-order {:serial serial :version 0})
                           :version 0})
            (k/where {:serial serial :version -1})))

(defn- delete-published!
  "Delete the published page. Only for internal use"
  [p]
  (if-let [p-published (get {:serial (:serial p) :version 1})]
    (let [objs-to-delete (group-by
                          #(:name %)
                          (k/select object (k/where {:page_id (:id p-published)})))]
      (doseq [[table objs] objs-to-delete]
        (let [table (get-object-entity table)]
         (k/delete table
                   (k/where {:object_id [in (map :id objs)]}))))
      (k/delete object (k/where {:page_id (:id p-published)}))
      (k/delete page
                (k/where {:serial (:serial p) :version 1})))))

(defn publish! [request]
  (let [p (dissoc (get (assoc request :version 0)) :attributes)
        objs-to-copy (group-by
                      #(:name %)
                      (k/select object
                                (k/where {:page_id (:id p)})
                                (k/order :id)))]
    ;; delete the published version
    (delete-published! p)
    ;; publish the edited version
    (let [p-new (k/insert page
                          (k/values (-> p
                                        util/revmap->str
                                        (dissoc :id :version)
                                        (assoc :published (k/sqlfn now))
                                        (assoc :version 1))))]
      (doseq [[table objs] objs-to-copy]
        (let [table (get-object-entity table)
              obj-data (k/select table
                                 (k/where {:object_id [in (map :id objs)]})
                                 (k/order :object_id))
              copied (reduce (fn [out obj]
                               (if (nil? obj)
                                 out
                                 (conj out (k/insert
                                            object
                                            (k/values (-> obj
                                                          (dissoc :id)
                                                          (assoc :page_id (:id p-new))))))))
                             []
                            objs)]
         (k/insert table (k/values (map
                                    (fn [t-data o-data]
                                      (-> t-data
                                          (dissoc :id)
                                          (assoc :object_id (:id o-data))))
                                    obj-data copied)))))
      ;; delete objects that are published elsewhere after a cut->paste operation
      (doseq [[table objs] objs-to-copy]
        (let [table (get-object-entity table)
              serial (:serial (first objs))
              other-pages (map :id (k/select page
                                             (k/fields :id)
                                             (k/where {:serial (:serial p-new)})))
              objs-to-delete (k/select object
                                       (k/where {:serial serial
                                                 :page_id [not-in other-pages]}))
              data-to-delete (k/select table
                                       (k/where {:object_id [in (map :id objs-to-delete)]}))]
          (k/delete table (k/where {:id [in (map :id data-to-delete)]}))
          (k/delete object (k/where {:id [in (map :id objs-to-delete)]})))))
    
    
    (update-route! (:uri p) (assoc (second (get-route (:uri p))) :published? true))
    request))

(defn unpublish! [request]
  (let [p (get (assoc request :version 0))]
    (delete-published! p)
    (update-route! (:uri p) (assoc (second (get-route (:uri p))) :published? false))
    request))

(defn published? [{:keys [serial id page-id]}]
  (let [id (or page-id id)]
    (if serial
      (not (zero? (count (k/select page (k/where {:serial serial :version 1})))))
      (= 1 (-> page (k/select (k/where {:id id})) first :version)))))

(defn updated! [{:keys [page-id serial]}]
  (let [w (if serial {:serial serial :version 0} {:id page-id})
        p (k/update page
                    (k/set-fields {:updated (k/sqlfn now)})
                    (k/where w))]
    (if-let [{:keys [user]} (get-in @settings [:edits (:uri p)])]
      (edit! (:uri p) user))))


(defn move! [anchor serial hit-mode]
  (let [{:keys [parent order uri name]} (get {:serial anchor :version 0})
        node (get {:serial serial :version 0})]
    (case hit-mode
      "before" (let [siblings (k/select page (k/where {:parent parent
                                                       :version 0
                                                       :order [> order]
                                                       :serial [not= serial]}))
                     new-uri (util/join-uri (util/uri-but-last-part uri)
                                            (util/uri-last-part (:uri node)))]

                 ;; update node
                 (k/update page
                           (k/set-fields {:order order :parent parent
                                          :uri new-uri})
                           (k/where {:serial serial :version 0}))
                 ;; update anchor
                 (k/update page
                           (k/set-fields {:order (+ order 1)})
                           (k/where {:serial anchor :version 0}))
                 ;; update siblings to new position after anchor and new node
                 (doseq [s siblings]
                   (k/update page
                             (k/set-fields {:order (+ (:order s) 2)})
                             (k/where {:serial (:serial s) :version 0})))
                 true)
      "after" (let [siblings (k/select page (k/where {:parent parent
                                                      :version 0
                                                      :order [> (+ order 1)]
                                                      :serial [not= serial]}))
                    new-uri (util/join-uri (util/uri-but-last-part uri)
                                            (util/uri-last-part (:uri node)))]
                ;; update node
                (k/update page
                          (k/set-fields {:order (+ order 1) :parent parent
                                         :uri new-uri})
                          (k/where {:serial serial :version 0}))
                ;; update siblings
                (doseq [s siblings]
                  (k/update page
                            (k/set-fields {:order (+ (:order s) 2)})
                            (k/where {:serial (:serial s) :version 0})))
                true)
      "over" (let [new-uri (util/join-uri uri (util/uri-last-part (:uri node)))]
               (k/update page
                         (k/set-fields {:order (get-last-order {:parent anchor})
                                        :parent anchor
                                        :uri new-uri})
                         (k/where {:serial serial :version 0}))
               true)
      false)))
