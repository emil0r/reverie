(ns reverie.core
  (:require [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.admin.tree :as tree]
            [reverie.admin.options :as options]
            [reverie.admin.options.page :as page]
            [reverie.dev :as dev]
            [reverie.meta :as meta]))



(defmulti init identity)
(defmethod init "/admin/frame/options/new-root-page" []
  (page/init))
(defmethod init :default []
  (meta/listen!)
  (meta/read! (fn []
                (if (:init-root-page? @meta/data)
                  (options/new-root-page!))
                (tree/init)
                (dev/start-repl))))

(jq/document-ready
 (fn []
   (init (-> js/window .-location .-pathname))))
