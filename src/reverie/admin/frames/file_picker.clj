(ns reverie.admin.frames.file-picker
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]
            [reverie.core :as rev]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [hiccup core form]
        
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.admin.modules.filemanager :only [join-paths
                                                  list-dir
                                                  get-mod-time
                                                  get-size]]
        [reverie.middleware :only [wrap-access]]
        [reverie.admin.templates :only [frame]]))


(defmulti row-file :type)
(defmethod row-file :directory [dir qs]
  [:tr
   [:td 
    [:a {:href (str
                (util/join-uri "/admin/frame/file-picker" (:url dir))
                "?" qs)}
     [:i.icon-folder-close]
     (:name dir)]]
   [:td]
   [:td (get-mod-time dir)]])
(defmethod row-file :file [file qs]
  [:tr
   [:td [:span.download {:url (:url file)} [:i.icon-download] (:name file)]]
   [:td (get-size file)]
   [:td (get-mod-time file)]])
(defmethod row-file :default [_ _])

(defn- file-lister [files {:keys [qs up? path]}]
  (frame
   (-> frame-options
       (assoc :title "File-picker: Images")
       (assoc :css ["/admin/css/font-awesome.min.css"
                    "/admin/css/main.css"]))
   [:div#files
    [:table.table
     [:tr
      [:th "Name"] [:th "Size"] [:th "Modified"]]
     (if up?
       (row-file {:type :directory
                  :name ".."
                  :url (str "/" (util/uri-but-last-part path))} qs))
     (map #(row-file % qs) files)]]))

(rev/defpage "/admin/frame/file-picker" {:middleware [[wrap-access :edit]]}
  [:get ["/images"]
   (file-lister (list-dir :images "") {:qs (:query-string request)})]
  [:get ["/images/:path" {:path #".*"}]
   (file-lister (list-dir :images path) {:qs (:query-string request)
                                             :up? true
                                             :path (str "images/" path)})])
