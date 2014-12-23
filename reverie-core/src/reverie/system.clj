(ns reverie.system
  (:refer-clojure :exclude [System])
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.route :as route]))

(defprotocol SystemProtocol
  (add-app-type! [system kw app])
  (add-page-type! [system kw page])
  (add-template-type! [system kw template])
  (add-object-type! [system kw object])
  (add-role-type! [system role])
  (objects [system])
  (templates [system])
  (apps [system])
  (pages [system])
  (roles [system]))



(defrecord ReverieSystem [database pages templates apps objects roles]
  component/Lifecycle
  (start [this]
    (if (and pages templates apps objects roles)
      this
      (assoc this
        :pages (atom {})
        :templates (atom {})
        :apps (atom {})
        :objects (atom {})
        :roles (atom []))))
  (stop [this]
    (if-not (and pages templates apps objects roles)
      this
,      (assoc this
        :pages nil
        :templates nil
        :apps nil
        :objects nil
        :roles nil)))

  SystemProtocol
  (add-app-type! [this kw app]
    )
  )
