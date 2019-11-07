(ns reverie.server
  "Handles the server instance starting and stopping"
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-server :refer [run-server]]
            [reverie.nsloader]
            [reverie.admin :as admin]
            [reverie.admin.api.editors :refer [get-edits-task]]
            [reverie.cache :as cache]
            [reverie.cache.memory :as cache.memory]
            [reverie.cache.sql :as cache.sql]
            [reverie.database.sql :as db.sql]
            [reverie.email :as email]
            [reverie.i18n :as i18n]
            [reverie.http.server :as server]
            [reverie.logger :as logger]
            [reverie.migrator :as migrator]
            [reverie.migrator.sql :as migrator.sql]
            [reverie.modules.filemanager :as fm]
            [reverie.modules.role :as rm]
            [reverie.page :as page]
            [reverie.scheduler :as scheduler]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [reverie.system :refer [load-views-ns] :as sys]
            [taoensso.timbre :as log]))



(defonce system (atom nil))

(defn- setup-dependencies
  "deps take the form of [dependancy data]"
  [internal-components external-components]
  ;; put the components into array-maps in order to keep
  ;; the order they've been put in
  ;; after that merge them in order for external to be able to override
  ;; internal components
  (->> (merge
        (into (array-map) internal-components)
        (into (array-map) external-components))
       ;; filter out anything that is to be removed
       (remove (fn [[dependancy opts-or-component]]
                 (= :remove opts-or-component)))
       ;; 
       (map (fn [[dependancy opts-or-component]]
              (if (record? opts-or-component)
                ;; it's an already existing component
                [dependancy opts-or-component]
                ;; get the component if it's a map 
                (let [{:keys [f args deps]} opts-or-component
                      args (if (map? args) [args] args)
                      component (if deps
                                  (component/using (apply f args) deps)
                                  (apply f args))]
                  [dependancy component]))))
       ;; flatten so that it can be applied to component/system-map
       (flatten)))

(defn- init-settings [{:keys [reverie.settings/settings reverie.settings/path]}]
  (-> (if path
        (settings/get-settings path)
        settings)
      (component/start)))

(defn system-map [opts components]
  ;; we're setting up db and i18n so that they can
  ;; load the messy io that other parts depend on
  ;; for db we need it before we can do any migrations
  (let [settings (init-settings opts)
        db (component/start (db.sql/database (settings/dev? settings)
                                             (settings/get settings [:database :specs])
                                             (settings/get settings [:database :ds-specs])))
        i18n- (component/start (i18n/get-i18n (settings/prod? settings)
                                              (settings/get settings [:i18n :tconfig]
                                                            {:dictionary {}
                                                             :dev-mode? (settings/dev? settings)
                                                             :fallback-locale :en})))]

    ;; run the migrations as the migrator is not a component
    (->> db
         (migrator.sql/get-migrator)
         (migrator/migrate))

    ;; load the translations for i18n
    (->> i18n-
         (i18n/load-i18n!))

    (let [internal-components [[:settings       settings]
                               [:database       db]
                               [:rolemanager    {:f rm/get-rolemanager
                                                 :deps [:database]}]
                               [:i18n           i18n-]
                               [:server         {:f server/get-server
                                                 :args {:server-options (settings/get settings [:http :server :options])
                                                        :run-server run-server
                                                        :stop-server server/stop-server
                                                        :dev? (settings/dev? settings)
                                                        :store (:reverie.http.server/store opts)}
                                                 :deps [:filemanager :site]}]
                               [:cachemanager   {:f cache/get-cachemanager
                                                 :args {:store (:reverie.cache/store opts)}
                                                 :deps [:database]}]
                               [:emailmanager   {:f email/email-manager
                                                 :args (settings/get settings [:email] {})}]
                               [:filemanager    {:f fm/get-filemanager
                                                 :args (settings/get settings [:filemanager])}]
                               [:site           {:f site/get-site
                                                 :args {:host-names (settings/get settings [:site :host-names])
                                                        :render-fn hiccup.compiler/render-html}
                                                 :deps [:database :cachemanager]}]
                               [:logger         {:f logger/logger
                                                 :args (merge
                                                        (settings/get settings [:log])
                                                        {:prod? (settings/prod? settings)})}]
                               [:scheduler      {:f scheduler/get-scheduler}]
                               [:admin          {:f admin/get-admin-initializer
                                                 :args {:store (:reverie.admin/store opts)}
                                                 :deps [:database]}]
                               [:system         {:f sys/get-system
                                                 :deps [:database :filemanager :site :scheduler
                                                        :settings :server :logger :emailmanager
                                                        :admin :cachemanager :i18n]}]]
          deps (setup-dependencies internal-components components)]
      (apply component/system-map deps))))


(defn stop []
  (when-not (nil? @system)
    (component/stop @system)
    (reset! system nil)))

(defn start [opts components]
  ;; load namespaces before the server starts up
  ;; this step will set up any necessary migrations
  (when-let [namespaces (:reverie.system/load-namespaces opts)]
    (assert (vector? namespaces) "(:reverie.system/load-namespaces opts) needs to be a vector")
    (apply load-views-ns namespaces))

  (reset! system (component/start (system-map opts components)))

  ;; start up the scheduler with tasks
  (let [scheduler (-> @system :scheduler)
        cachemanager (-> @system :cachemanager)
        settings (-> @system :settings)]
    (scheduler/add-tasks!
     scheduler
     [(get-edits-task
       (settings/get settings [:admin :tasks :edits :minutes] 30))
      (cache/get-prune-task
       (cache.sql/get-basicstrategy) cachemanager {})])
    (scheduler/start! scheduler))

    ;; shut down the system if something like ctrl-c is pressed
    (.addShutdownHook
     (Runtime/getRuntime)
     (proxy [Thread] []
       (run []
         (stop)))))
