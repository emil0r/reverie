(ns reverie.admin.app
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.meta :as meta])
  (:use [reverie.util :only [ev$]]))


(defn show-navigation []
  (-> :#app-navigation jq/$ (jq/remove-class "hidden")))

(defn hide-navigation []
  (-> :#app-navigation jq/$ (jq/add-class "hidden")))


(defn handle-navigation [node-data]
  (if (and (= (:type node-data) "app") (= (:app-type node-data) "template"))
    (do
      (let [app (get-in @meta/data [:apps (keyword (:app node-data))])
            $ul (jq/$ "#app-navigation > ul")]
        (jq/empty $ul)
        (doseq [[path help] (:paths app)]
          (jq/append $ul
                     (crate/html
                      [:li [:span (if (= path "*")
                                    {:class "active"}
                                    {}) path] help]))))
      (show-navigation))
    (hide-navigation)))

(defn clear-app-paths! []
  (doseq [path (jq/$ "#app-navigation > ul > li > span")]
    (-> path jq/$ (jq/remove-class "active"))))

(defn click-app-path! [e]
  (let [$e (ev$ e)]
    (clear-app-paths!)
    (jq/add-class $e "active")))

(defn listen! []
  (-> :#app-navigation
      jq/$
      (jq/delegate "ul>li>span" :click click-app-path!)))
