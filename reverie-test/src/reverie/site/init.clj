(ns reverie.site.init
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [digest :refer [md5]]
            [org.httpkit.server :as http-server :refer [run-server]]
            reverie.nsloader
            [reverie.admin :as admin]
            [reverie.admin.api.editors :refer [get-edits-task]]
            [reverie.cache :as cache]
            [reverie.cache.memory :as cache.memory]
            [reverie.cache.sql :as cache.sql]
            [reverie.database.sql :as db.sql]
            [reverie.logger :as logger]
            [reverie.migrator :as migrator]
            [reverie.migrator.sql :as migrator.sql]
            [reverie.modules.filemanager :as fm]
            [reverie.modules.role :as rm]
            [reverie.page :as page]
            [reverie.scheduler :as scheduler]
            [reverie.server :as server]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [reverie.system :refer [load-views-ns] :as sys]))


(defn- system-map [{:keys [prod? log db-specs settings
                           host-names render-fn
                           base-dir media-dirs
                           cache-store site-hash-key-strategy
                           server-options middleware-options
                           run-server stop-server]}]
  (let [db (component/start (db.sql/database db-specs))]
    ;; run the migrations for reverie/CMS
    (->> db
         (migrator.sql/get-migrator)
         (migrator/migrate))

    (component/system-map
     :database db
     :settings settings
     :rolemanager (component/using (rm/get-rolemanager)
                                   [:database])
     :server (component/using (server/get-server {:server-options server-options
                                                  :run-server run-server
                                                  :stop-server stop-server
                                                  :dev? (not prod?)})
                              [:filemanager :site])
     :cachemanager (component/using
                    (cache/cachemananger {:store cache-store})
                    [:database])
     :filemanager (fm/get-filemanager base-dir media-dirs)
     :site (component/using (site/site {:host-names host-names
                                        :render-fn render-fn})
                            [:database :cachemanager])
     :logger (logger/logger prod? (:rotor log))
     :scheduler (scheduler/get-scheduler)
     :admin (component/using (admin/get-admin-initializer)
                             [:database])
     :system (component/using (sys/get-system)
                              [:database :filemanager :site :scheduler
                               :settings :server :logger
                               :admin :cachemanager]))))


(defonce system (atom nil))

(defn stop []
  (when-not (nil? @system)
    (component/stop @system)
    (reset! system nil)))

(defn- stop-server [server]
  (when (fn? server)
    (server)))

(defn init [settings-path]
  ;; read in the settings first
  (let [settings (component/start (settings/settings settings-path))]

    ;; start the system
    (reset! system (component/start
                    (system-map
                     {:prod? (settings/prod? settings)
                      :log (settings/get settings [:log])
                      :settings settings
                      :db-specs (settings/get settings [:db :specs])
                      :server-options (settings/get settings [:server :options])
                      :middleware-options (settings/get settings [:server :middleware])
                      :run-server run-server
                      :stop-server stop-server
                      :host-names (settings/get settings [:site :host-names])
                      :render-fn hiccup.compiler/render-html
                      :base-dir (settings/get settings [:filemanager :base-dir])
                      :media-dirs (settings/get settings [:filemanager :media-dirs])
                      :cache-store (cache.memory/mem-store)})))

    ;; load namespaces after the system starts up
    ;; this step will set up any necessary migrations
    (load-views-ns 'reverie.sql.objects
                   'reverie.site.templates
                   'reverie.site.apps
                   'reverie.site.endpoints)

    ;; run the migrations that now have been defined by the loaded modules, objects, etc
    (->> @system
         :database
         (migrator.sql/get-migrator)
         (migrator/migrate))

    ;; start up the scheduler with tasks
    (let [scheduler (-> @system :scheduler)
          cachemanager (-> @system :cachemanager)]
      (scheduler/add-tasks!
       scheduler
       [(get-edits-task
         (settings/get settings [:admin :tasks :edits :minutes]))
        (cache/get-prune-task
         (cache.sql/get-basicstrategy) cachemanager {})])
      (scheduler/start! scheduler))

    ;; shut down the system if something like ctrl-c is pressed
    (.addShutdownHook
     (Runtime/getRuntime)
     (proxy [Thread] []
       (run []
         (stop))))))
