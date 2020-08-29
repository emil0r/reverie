(ns reverie.http.router
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as reverie.db]
            [reverie.http.route :as route]
            [reverie.system :as sys]
            [taoensso.timbre :as log]))

(defonce static-routes (atom {}))

(defprotocol IRouter
  (add-route [router route page-properties])
  (reset-routes! [router])
  (get-page [router request]))

(extend-type nil
  IRouter
  (add-route [_ _ _] nil)
  (reset-routes! [_] nil)
  (get-page [router request] nil))

(defn- get-route+routes [uri routes-atom-seq]
  (reduce (fn [out routes]
            (let [routes* @routes]
              (if-let [data (get routes* uri)]
                (reduced [data routes*])
                nil)))
          nil routes-atom-seq))

(defn get-route+properties [uri routes]
  (if-let [data (get routes uri)]
    data
    (->>
     @static-routes
     (filter (fn [[k [r properties]]]
               (re-find (re-pattern (str "^" k)) uri)))
     (sort-by first)
     reverse
     first
     second)))

(defrecord Router [database started? routes middleware]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting Router")
          (let [this* (assoc this
                             :started? true
                             :routes (atom {}))]
            (reset-routes! this*)
            this*))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping Router")
          (reset! routes {})
          (assoc this
                 :started? false))))
  IRouter
  (add-route [router route properties]
    (swap! routes assoc (:path route) [route properties]))
  (reset-routes! [router]
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
                 (reverie.db/get-pages-by-route database)))))
  (get-page [router {:keys [uri] :as request}]
    (let [[route properties] (get-route+properties uri @routes)]
      (if route
        (let [{:keys [template app type name serial]} properties
              public? (not (get-in request [:reverie :editor?]))]
          (case type
            :page (let [p (reverie.db/get-page database serial public?)]
                    (if (and p
                             (= (:path route) (-> p :route :path)))
                      (assoc p :route route)
                      nil))
            :raw (let [page (sys/raw-page name)]
                   (assoc page :database database))
            :module (assoc (:module (sys/module name))
                           :route route
                           :database database)
            :app (let [p (reverie.db/get-page database serial public?)]
                   (if (and p
                            (= (:path route) (-> p :route :path)))
                     (assoc p :route route)
                     nil))))
        nil))))


(defn router [settings]
  (map->Router settings))
