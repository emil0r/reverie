(ns reverie.core
  (:require [clojure.string :as s]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.admin.area :as area]
            [reverie.admin.filemanager :as filemanager]
            [reverie.admin.file-picker :as file-picker]
            [reverie.admin.options :as options]
            [reverie.admin.options.object :as object]
            [reverie.admin.options.page :as page]
            [reverie.misc :as misc]
            [reverie.admin.tree :as tree]
            [reverie.dom :as dom]
            [reverie.dev :as dev]
            [reverie.meta :as meta]))



(defmulti init (fn [loc]
                 (cond
                  (= "/admin/frame/options/new-root-page" loc) :root-page
                  (= "/admin/frame/options/add-page" loc) :add-page
                  (= "/admin/frame/options/restore" loc) :restore
                  (= "/admin/frame/options/delete" loc) :delete
                  (= "/admin/frame/object/edit" loc) :object-edit
                  (re-find #"^/admin/frame/filemanager" loc) :filemanager
                  (re-find #"^/admin/frame/file-picker" loc) :file-picker)))
(defmethod init :root-page []
  (page/init))
(defmethod init :add-page []
  (page/init))
(defmethod init :restore []
  (page/init))
(defmethod init :delete []
  (page/init))
(defmethod init :object-edit []
  (object/init))
(defmethod init :filemanager []
  (filemanager/init))
(defmethod init :file-picker []
  (file-picker/init))
(defmethod init :default []
  (meta/listen!)
  (tree/listen!)
  (area/init)
  (misc/listen!)
  (dom/$m-loaded #(dom/$m-ready area/init))
  (meta/read! (fn []
                (if (:init-root-page? @meta/data)
                  (options/new-root-page!))
                (tree/init)
                (dev/start-repl))))

(jq/document-ready
 (fn []
   (init (-> js/window .-location .-pathname))))
