(ns reverie.admin.options.page
  (:require [clojure.string :as string]
            [jayq.core :as jq]
            [jayq.util :as util]))


(defn normalize [s]
  (-> s
      (string/replace #"[åÅäÄĀāĀāÀÁÂÃÆàáâãæ]" "a")
      (string/replace #"[ČčÇç]" "c")
      (string/replace #"[Ðð]" "d")
      (string/replace #"[ĒēĒēËëÈÉÊËèéêë]" "e")
      (string/replace #"[Ğğ]" "g")
      (string/replace #"[ĪīĪīÏïİıìíîïÌÍÎÏ]" "i")
      (string/replace #"[Ĳĳ]" "ij")
      (string/replace #"[Ññ]" "n")
      (string/replace #"[öÖŐőŌōŌōŒœŒœòóôõöøÒÓÔÕÖØ]" "o")
      (string/replace #"[Þþ]" "p")
      (string/replace #"[Řř]" "r")
      (string/replace #"[ŠšŠšŠŞşŠš]" "s")
      (string/replace #"[ß]" "ss")
      (string/replace #"[ŰűŪūŪūÜüÙÚÛÜùúûü]" "u")
      (string/replace #"[ẀẁẂẃŴŵ]" "w")
      (string/replace #"[ŶŷŸýÝÿŸ]" "y")
      (string/replace #"[ŽžŽžŽžžŽ]" "z")
      (string/replace #"\s" "-")
      (string/replace #"\&" "-")
      (string/replace #"[^a-zA-Z0-9\-\_\.]" "")
      string/lower-case))

(defn switch-template-app [e]
  (-> :tr.template jq/$ (jq/toggle-class "hidden"))
  (-> :tr.app jq/$ (jq/toggle-class "hidden")))

(defn change-uri []
  (util/log "change-uri")
  (let [name (-> :#name jq/$ jq/val)]
    (util/log name)
    (-> :#uri jq/$ (jq/val (normalize name)))))

(defn init-templates-app []
  (if (= "app" (-> :#type jq/$ jq/val))
    (switch-template-app nil)))

(defn init []
  (init-templates-app)
  (-> :#type jq/$ (jq/bind :change switch-template-app))
  (-> :#name jq/$ (jq/bind :change change-uri)))
