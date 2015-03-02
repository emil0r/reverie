(ns reverie.system
  (:refer-clojure :exclude [System])
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.route :as route]))

(defonce storage (atom {:raw-pages {}
                        :apps {}
                        :objects {}
                        :templates {}
                        :roles {}
                        :modules {}
                        :module-default-routes []
                        :migrations {}
                        :database nil}))

(defn get-db []
  (:database @storage))

(defprotocol ISystem
  (add-object-type! [system key object-type])
  (objects [system])
  (object [system key])

  (add-template-type! [system key template-type])
  (templates [system])
  (template [system key])

  (add-app-type! [system key app-type])
  (apps [system])
  (app [system key])

  (add-raw-page-type! [system key raw-page-type])
  (raw-pages [system])
  (raw-page [system key])

  (add-role-type! [system key role-type])
  (roles [system])
  (role [system key])

  (modules [system])
  (module [system key])

  (migrations [system])
  (migration [system key]))



(defrecord ReverieSystem [database]
  component/Lifecycle
  (start [this]
    (swap! storage assoc :database database)
    this)
  (stop [this]
    (swap! storage assoc :database nil)
    this)

  ISystem
  (add-app-type! [this key app-type]
    (swap! storage assoc-in [:apps key] app-type))
  (apps [this]
    (:apps @storage))
  (app [this key]
    (get-in @storage [:apps key]))

  (add-template-type! [this key template-type]
    (swap! storage assoc-in [:templates key] template-type))
  (templates [this]
    (:templates @storage))
  (template [this key]
    (get-in @storage [:templates key]))

  (add-raw-page-type! [this key raw-page-type]
    (swap! storage assoc-in [:raw-pages key] raw-page-type))
  (raw-pages [this]
    (:raw-pages @storage))
  (raw-page [this key]
    (get-in @storage [:raw-pages key]))

  (add-role-type! [this key role-type]
    (swap! storage assoc-in [:roles key] role-type))
  (roles [this]
    (:roles @storage))
  (role [this key]
    (get-in @storage [:roles key]))

  (add-object-type! [this key object-type]
    (swap! storage assoc-in [:objects key] object-type))
  (objects [this]
    (:objects @storage))
  (object [this key]
    (get-in @storage [:objects key]))

  (modules [this]
    (:modules @storage))
  (module [this key]
    (get-in @storage [:modules key]))

  (migrations [this]
    (:migrations @storage))
  (migration [this key]
    (get-in @storage [:migrations key])))


(defn get-system []
  (ReverieSystem. nil))
