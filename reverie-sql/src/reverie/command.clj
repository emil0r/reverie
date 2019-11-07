(ns reverie.command
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [honeysql.core :as sql]
            [reverie.auth :as auth]
            [reverie.auth.sql]
            [reverie.database.sql :as db.sql]
            [reverie.migrator :as migrator]
            [reverie.migrator.sql :as migrator.sql]
            [reverie.modules.auth]
            [reverie.modules.role :as role]
            [reverie.settings :as settings]
            [reverie.system :refer [load-views-ns]]
            [taoensso.timbre :as log]))

(defn- print-info
  ([s] (print-info s false))
  ([s expand?]
   (let [to-print (if expand? ["\n\n" s "\n-----\n\n"] ["\n\n" s "\n\n"])]
     (apply println to-print))))

(defn- get-settings [path]
  (component/start (settings/get-settings path)))

(defn- get-db [settings]
  (let [dev? (settings/dev? settings)
        db-specs (settings/get settings [:database :specs])
        ds-specs (settings/get settings [:database :ds-specs])]
    (component/start (db.sql/database dev? db-specs ds-specs))))

(defn- read-input [info]
  (print-info info)
  (read-line))

(defn- command-superuser [opts db args]
  (print-info "Adding new superuser" true)
  (let [full-name (read-input "Full name?")
        spoken-name (read-input "Spoken name?")
        username (read-input "User name? (This is what you will log in with)")
        email (read-input "Email?")
        password (read-input "Password?")
        passes? (cond
                 (str/blank? email) "Blank email"
                 (str/blank? username) "Blank username"
                 (str/blank? password) "Blank password"
                 (not (zero? (count (->> (db/query db {:select [:*]
                                                       :from [:auth_user]
                                                       :where [:= :username username]}))))) "User already exists"
                                                       :else true)]
    (if (true? passes?)
      (do
        (-> (role/get-rolemanager)
            (assoc :database db)
            (component/start)
            (component/stop))
        (auth/add-user! {:username username
                         :password password
                         :email email
                         :full_name full-name
                         :spoken_name spoken-name} #{:admin} nil db)
        (print-info (str "Superuser " username " added")))
      (print-info (str "Could not add new superuser: " passes?)))))

(defn- command-migrate [opts db args]
  (print-info "Migrating..." true)
  (->> db
       (migrator.sql/get-migrator)
       (migrator/migrate))
  (print-info "Migration done..."))


(defn- command-root-page [opts db args]
  (print-info "Adding root page" true)
  (if (->> (db/query db {:select [:%count.*]
                         :from [:reverie_page]})
           first :count zero?)
   (let [name (read-input "Name?")
         title (read-input "Title? (blank is ok)")]
     (if (str/blank? name)
       (do
         (print-info "Name can't be blank")
         (command-root-page db args))
       (do
         (db/query! db {:insert-into :reverie_page
                        :values [{:name name
                                  :title title
                                  :type "page"
                                  :app ""
                                  :slug "/"
                                  :route "/"
                                  :parent nil
                                  :serial 1
                                  :version 0
                                  (sql/raw "\"order\"") 1
                                  :template "main"}]})
         (print-info (str "Root page " name " added")))))
   (print-info "Root page already exists. Aborting...")))

(defn- command-init [opts db args]
  (command-migrate opts db args)
  (command-root-page opts db args)
  (command-superuser opts db args))

(defn- command-help []
  (println "
------------
commands are
-----------

:init      - initialize reverie (run :migrate, :root-page and :superuser in succession)
:migrate   - migrate the database
:root-page - add a root page to the database
:superuser - create a superuser
:help      - print this

"))

(defn run-command [{:keys [reverie.settings/path
                           reverie.system/load-namespaces] :as opts} args]
  (let [[command? command & args] (map edn/read-string args)]
    (when (= :command command?)
      (log/set-level! :info)
      (if (or (= command :help)
              (nil? command))
        (command-help)
        (let [settings (get-settings path)
              db (get-db settings)]
          (when load-namespaces
            (assert (vector? load-namespaces) "(:reverie.system/load-namespaces opts) needs to be a vector")
            (apply load-views-ns load-namespaces))
          (case command
            :init (command-init opts db args)
            :migrate (command-migrate opts db args)
            :root-page (command-root-page opts db args)
            :superuser (command-superuser opts db args)
            :help (command-help)
            (do (print-info (str "Command " command " not found"))
                (command-help)))
          (component/stop db)
          (component/stop settings)))
      ;; exit the system as commands are meant to be run from the command line
      (System/exit 0))))
