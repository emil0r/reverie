(ns reverie.helpers
  (:use [reverie.atoms :only [templates]]))

(defn with-template
  "Used for defapp, defmodule, defpage when you want to reuse a defined template"
  [template request areas]
  (let [template-fn (:fn (@templates template))]
    (:body (template-fn (-> request
                            (assoc-in [:reverie :overriden] :with-template)
                            (assoc-in [:reverie :overridden/areas] areas))))))
