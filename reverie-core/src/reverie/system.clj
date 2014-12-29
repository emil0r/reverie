(ns reverie.system
  (:refer-clojure :exclude [System])
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.route :as route]))

(defonce storage (atom {:raw-pages {}
                        :apps {}
                        :objects {}
                        :templates {}
                        :roles {}}))

(defprotocol SystemProtocol
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
  (role [system key]))



(defrecord ReverieSystem [database raw-pages apps objects templates roles]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this)

  SystemProtocol
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
    (get-in @storage [:objects key])))
