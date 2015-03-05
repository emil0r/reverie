(ns reverie.test.server
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.client :as client]
            [org.httpkit.server :as http-server :refer [run-server]]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [reverie.test.database.sql-helpers :refer [get-db seed!]]
            reverie.test.helpers
            reverie.sql.objects.text
            reverie.sql.objects.image
            reverie.admin.index
            [reverie.auth :as auth]
            reverie.modules.auth
            [reverie.modules.filemanager :as fm]
            [reverie.render :as render]
            [reverie.server :as server]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [reverie.system :as sys]))


(defn- stop-server [server]
  (when (fn? server)
    (server)))

(let [db (component/start (get-db))
      settings (-> "test/reverie/test/settings.edn"
                   settings/settings
                   component/start)
      site (component/start (site/site
                             {:host-names []
                              :database db
                              :system (:system db)
                              :render-fn (fn [data] (hiccup.compiler/render-html data))}))
      server (component/start
              (reverie.server/get-server {:dev? true
                                          :site site
                                          :server-options (settings/get settings [:server-options])
                                          :run-server run-server
                                          :stop-server stop-server}))]
  (try
    ;; @(client/post "http://127.0.0.1:9090/admin/login"
    ;;                      {:form-params {:username "admin"
    ;;                                     :password "admin"}})
    #spy/d @(client/get "http://127.0.0.1:9090/")
    ;; #spy/d (render/render site {:uri "/" :request-method :get})
    (catch Exception e
      (println e)))
  (component/stop db)
  (component/stop settings)
  (component/stop server))


(defonce ^:private test-server (atom {}))
(defn- start-test-server []
  (try
    (let [db (component/start (get-db))
          settings (-> "test/reverie/test/settings.edn"
                       settings/settings
                       component/start)
          filemanager (component/start
                       (fm/get-filemanager "media"
                                           ["media/images"
                                            "media/files"]))
          site (component/start (site/site
                                 {:host-names []
                                  :database db
                                  :system (:system db)
                                  :render-fn (fn [data] (hiccup.compiler/render-html data))}))
          site (assoc site :db db)
          system (component/start (assoc (sys/get-system)
                                    :database db
                                    :site site
                                    :filemanager filemanager))
          db (assoc db :system system)
          server (component/start
                  (reverie.server/get-server {:dev? true
                                              :site site
                                              :server-options (settings/get settings [:server-options])
                                              :run-server run-server
                                              :stop-server stop-server
                                              :filemanager filemanager}))]
      (reset! test-server {:server server
                           :site site
                           :db db
                           :settings settings}))
    (catch Exception e
      (println e))))

(defn- stop-test-server []
  (do
    (when-let [db (:db @test-server)]
      (component/stop db))
    (when-let [filemanager (-> @test-server :db :system :filemanager)]
      (component/stop filemanager))
    (when-let [system (:system (:db @test-server))]
      (component/stop system))
    (when-let [settings (:settings @test-server)]
      (component/stop settings))
    (when-let [site (:site @test-server)]
      (component/stop site))
    (when-let [server (:server @test-server)]
      (component/stop server))))


(comment

  (do (stop-test-server) (start-test-server))

  (stop-test-server)

  (start-test-server))


(-> @(client/get "http://127.0.0.1:9090/admin/frame/module/auth"
                 )
    ;;:body
    ;;slurp
    )
