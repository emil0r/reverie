(ns reverie.system
  (:refer-clojure :exclude [System])
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.route :as route]))

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
    (if (and raw-pages templates apps objects roles)
      this
      (assoc this
        :raw-pages (atom {})
        :templates (atom {})
        :apps (atom {})
        :objects (atom {})
        :roles (atom []))))
  (stop [this]
    (if-not (and raw-pages templates apps objects roles)
      this
,      (assoc this
         :raw-pages nil
         :templates nil
         :apps nil
         :objects nil
         :roles nil)))

  SystemProtocol
  (add-app-type! [this key app-type]
    (swap! apps assoc key app-type))
  (apps [this]
    @apps)
  (app [this key]
    (get @apps key))
  (add-template-type! [this key template-type]
    (swap! templates assoc key template-type))
  (templates [this]
    @templates)
  (template [this key]
    (get @templates key))
  (add-raw-page-type! [this key raw-page-type]
    (swap! raw-pages assoc key raw-page-type))
  (raw-pages [this]
    @raw-pages)
  (raw-page [this key]
    (get @raw-pages key))
  (add-role-type! [this key role-type]
    (swap! roles assoc key role-type))
  (roles [this]
    @roles)
  (role [this key]
    (get @roles key))
  (add-object-type! [this key object-type]
    (swap! objects assoc key object-type))
  (objects [this]
    @objects)
  (object [this key]
    (get @objects key)))
