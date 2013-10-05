(ns reverie.core
  (:require [clojure.string :as s]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.admin.area :as area]
            [reverie.admin.filemanager :as filemanager]
            [reverie.admin.file-picker :as file-picker]
            [reverie.admin.url-picker :as url-picker]
            [reverie.admin.options :as options]
            [reverie.admin.options.object :as object]
            [reverie.admin.options.page :as page]
            [reverie.admin.tabs :as tabs]
            [reverie.admin.tree :as tree]
            [reverie.dev :as dev]
            [reverie.dom :as dom]
            [reverie.meta :as meta]
            [reverie.misc :as misc]))



(defmulti init (fn [loc]
                 (cond
                  (= "/admin/frame/options/new-root-page" loc) :root-page
                  (= "/admin/frame/options/add-page" loc) :add-page
                  (= "/admin/frame/options/meta" loc) :meta
                  (= "/admin/frame/options/restore" loc) :restore
                  (= "/admin/frame/options/delete" loc) :delete
                  (= "/admin/frame/object/edit" loc) :object-edit
                  (re-find #"^/admin/frame/module/filemanager" loc) :filemanager
                  (re-find #"^/admin/frame/file-picker" loc) :file-picker
                  (re-find #"^/admin/frame/url-picker" loc) :url-picker
                  (re-find #"^/admin/frame/url-picker/files" loc) :url-picker-files
                  (re-find #"^/admin/frame/module" loc) :module)))
(defmethod init :root-page []
  (page/init))
(defmethod init :add-page []
  (page/init))
(defmethod init :meta []
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
(defmethod init :url-picker []
  (url-picker/init))
(defmethod init :url-picker-files []
  (url-picker/init-files))
(defmethod init :module [])
(defmethod init :default []
  (meta/listen!)
  (tree/listen!)
  (misc/listen!)
  (area/init)
  (tabs/init)
  (dom/$m-loaded #(dom/$m-ready area/init))
  (meta/read! (fn []
                (if (:init-root-page? @meta/data)
                  (options/new-root-page!))
                (tree/init)
                (dev/start-repl))))

(jq/document-ready
 (fn []
   (init (-> js/window .-location .-pathname))))
