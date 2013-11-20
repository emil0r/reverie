(ns reverie.admin.options.page
  (:require [clojure.string :as string]
            [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [normalize]]))




(defn switch-template-app [e]
  (-> :tr.app jq/$ (jq/toggle-class "hidden")))

(defn change-uri []
  (let [name (-> :#name jq/$ jq/val)]
    (-> :#uri jq/$ (jq/val (normalize name)))))

(defn init-templates-app []
  (if (= "app" (-> :#type jq/$ jq/val))
    (switch-template-app nil)))

(defn init []
  (init-templates-app)
  (-> :#type jq/$ (jq/bind :change switch-template-app))
  (-> :#name jq/$ (jq/bind :change change-uri)))
