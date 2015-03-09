(ns reverie.site
  (:require [com.stuartsierra.component :as component]
            [reverie.admin.api.editors :as editors]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.response :as response]
            [reverie.route :as route]
            [reverie.system :as sys]
            [slingshot.slingshot :refer [try+]])
  (:import [reverie RenderException]))

(defonce routes (atom {}))

(defprotocol ISite
  (add-route! [site route page-data])
  (reset-routes! [site])
  (get-page [site request])
  (host-match? [site request])
  (set-system-page! [site status rendered-page]))


(defrecord Site [host-names system
                 system-pages settings database render-fn]
  component/Lifecycle
  (start [this]
    (reset-routes! this)
    this)
  (stop [this] this)

  ISite
  (add-route! [this route properties]
    (swap! routes assoc (:path route) [route properties]))
  (reset-routes! [this]
    (swap! routes merge
           (into
            {}
            (map (fn [[route properties]]
                   {route [(route/route [route]) (select-keys properties
                                                              [:template
                                                               :app
                                                               :type
                                                               :name
                                                               :serial])]})
                 (db/get-pages-by-route database)))))
  (get-page [this {:keys [reverie] :as request}]
    (let [uri (:uri request)]
      (let [[route properties]
            (if-let [data (get @routes uri)]
              data
              (->>
               @routes
               (filter (fn [[k [r properties]]]
                         (and (not= (:type properties) :page)
                              (re-find (re-pattern (str "^" k)) uri))))
               (sort-by first)
               reverse
               first
               second))]
        (if route
          (let [{:keys [template app type name serial]} properties
                public? (not (get-in request [:reverie :editor?]))]
            (case type
              :page (let [p (db/get-page database serial public?)]
                      (if (and p
                               (= (:path route) (-> p :route :path)))
                        (assoc p :route route)
                        nil))
              :raw (let [page-data (sys/raw-page name)]
                     (page/raw-page
                      {:route route
                       :options (:options page-data)
                       :routes (:routes page-data)
                       :database database}))
              :module (assoc (:module (sys/module name))
                        :route route
                        :database database)
              :app (let [p (db/get-page database serial public?)]
                     (if (and p
                              (= (:path route) (-> p :route :path)))
                       (assoc p :route route)
                       nil))))))))

  (host-match? [this {:keys [server-name]}]
    (if (empty? host-names)
      true
      (some #(= server-name %) host-names)))

  render/IRender
  (render [this request]
    (try+
     (if-not (host-match? this request)

       (or (get system-pages 404)
           (response/get 404)) ;; no match for against the host names -> 404
       (if-let [p (get-page this request)]
         (do
           (editors/edit-follow! p (get-in request [:reverie :user]))
           (let [request (assoc-in request [:reverie :edit?]
                                   (editors/edit? p (get-in request [:reverie :user])))]
             (if-let [resp (render/render p request)]
               (editors/assoc-admin-links
                p
                request
                (if (map? resp)
                  (assoc resp :body (render-fn (:body resp)))
                  {:status 200
                   :body (render-fn resp)
                   :headers {"Content-Type" "text/html; charset=utf-8;"}}))
               (or (get system-pages 404)
                   (response/get 404))))) ;; got back nil -> 404
         (or (get system-pages 404)
             (response/get 404)))) ;; didn't find page -> 404
     (catch [:type :response] {:keys [status args]}
       (or
        (response/get (get system-pages status))
        (apply response/get status args)))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.site/Site"))))


(defn site [data]
  (map->Site data))
