# init.clj

init.clj is the file which holds all startup details of reverie and the site.


## system-map

This functions creates the system map for reverie using the [component](https://github.com/stuartsierra/component) library.

The following things need to happen.

- The database needs to be started first.
- The migrations needs to be run right after so that any reverie specific migrations that has not yet been applied, can be applied before anything else runs.
- The component/system-map needs to be generated


```clojure

;; start database first
(let [db (component/start (db.sql/database db-specs))]
  ;; run the migrations for reverie/CMS
  (->> db
       (migrator.sql/get-migrator)
       (migrator/migrate))

  (component/system-map
    ;; set database
    :database db
    
    ;; set settings
    :settings settings
    
    ;; add any missing roles to the database
    :rolemanager (component/using (rm/get-rolemanager)
                                  [:database])
                                  
    ;; start i18n. this includes loading defined i18n dictionaries
    ;; from objects, endpoints, apps and modules
    :i18n (component/using (i18n/get-i18n prod? i18n-tconfig) [])
    
    ;; this starts the HTTP Server
    :server (component/using (server/get-server {;; options sent to the server
                                                 :server-options server-options

                                                 ;; function for running the server
                                                 :run-server run-server
                                                 
                                                 ;; function for stopping the server
                                                 :stop-server stop-server
                                                 
                                                 ;; dev mode?
                                                 :dev? (not prod?)})
                             [:filemanager :site])
                             
    ;; cache manager
    :cachemanager (component/using
                   (cache/cachemananger {:store cache-store})
                   [:database])
                   
    ;; file manager
    :filemanager (fm/get-filemanager base-dir media-dirs)
    
    ;; this little nugget handles the rendering of the 'site'
    ;; a fair amount of rendering logic is tangled up in here
    ;; this is also where the render-fn gets set
    :site (component/using (site/site {:host-names host-names
                                       :render-fn render-fn})
                           [:database :cachemanager])
                           
    ;; how do we log things
    :logger (logger/logger prod? (:rotor log))
    
    ;; scheduler
    :scheduler (scheduler/get-scheduler)
    
    ;; admin interface
    :admin (component/using (admin/get-admin-initializer)
                            [:database])
                            
    ;; save all important stuff to the reverie.system namespace
    :system (component/using (sys/get-system)
                             [:database :filemanager :site :scheduler
                              :settings :server :logger
                              :admin :cachemanager :i18n])))
```


## init

This function is responsible for initiliaze settings, run the system-map, load namespaces during startup, run any extra migrations, set up the scheduler and add the system shutdown hook.

This function is also where you can add any extra initialization of your own.
