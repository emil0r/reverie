(ns reverie.site
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.response :as response]
            [reverie.route :as route]
            [reverie.system :as sys]
            [slingshot.slingshot :refer [try+]]))


(defprotocol SiteProtocol
  (add-route! [system route page-data])
  (get-page [system request])
  (host-match? [system request])
  (set-system-page! [system status rendered-page])
  (move-object! [system page object area order])
  (move-page! [system page order]))


(defrecord Site [host-names routes system
                 system-pages settings database render-fn]
  component/Lifecycle
  (start [this]
    (if routes
      this
      (assoc this
        :routes (atom (into
                       {}
                       (map (fn [[route properties]]
                              {route [(route/route [route]) properties]})
                            (db/get-pages-by-route database)))))))
  (stop [this]
    (if-not routes
      this
      (assoc this
        :routes nil
        :system-pages nil)))

  SiteProtocol
  (add-route! [this route properties]
    (swap! routes assoc (:path route) [route properties]))
  (get-page [this request]
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
          (let [{:keys [template app type name]} properties]
            (case type
              :page (let [page (db/get-page database (:id properties))
                          objects (map
                                   (fn [obj]
                                     (let [obj-data (sys/object system (object/name obj))]
                                      (assoc obj
                                        :methods (:methods obj-data)
                                        :properties (:properties obj-data))))
                                   (db/get-objects database page))]
                      (page/page
                       (assoc page
                         :template (sys/template system template)
                         :database database
                         :route route
                         :objects objects)))
              :raw (let [page-data (sys/raw-page system name)]
                     (page/raw-page
                      {:route route
                       :properties (:properties page-data)
                       :methods (:methods page-data)
                       :database database}))
              :app (let [page (db/get-page database (:id properties))
                         page-data (sys/app system app)
                         objects (map
                                  (fn [obj]
                                    (assoc obj
                                      :methods (:methods (sys/object system (object/name obj)))
                                      :route (route/route [(:route obj)])))
                                  (db/get-objects database page))]
                     (page/app-page
                      (assoc page
                        :template (sys/template system template)
                        :properties (:properties page-data)
                        :app-routes (:app-routes page-data)
                        :database database
                        :route route
                        :objects objects)))))))))

  (host-match? [this {:keys [server-name]}]
    (if (empty? host-names)
      true
      (some #(= server-name %) host-names)))

  render/RenderProtocol
  (render [this request]
    (try+
     (if-not (host-match? this request)
       (or (get system-pages 404)
           (response/get 404)) ;; no match for against the host names -> 404
       (if-let [p (get-page this request)]
         (if-let [resp (render/render p request)]
           (assoc resp :body (render-fn (:body resp)))
           (or (get system-pages 404)
               (response/get 404))) ;; got back nil -> 404
         (or (get system-pages 404)
             (response/get 404)))) ;; didn't find page -> 404
     (catch [:type :response] {:keys [status args]}
       (or
        (response/get (get system-pages status))
        (apply response/get status args))))))


(defn site [data]
  (map->Site data))
