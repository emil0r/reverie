(ns reverie.admin.frames.file-picker
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]
            [reverie.core :as rev]
            [reverie.response :as r]
            [reverie.util :as util])
  (:use [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.admin.modules.filemanager :only [join-paths
                                                  list-dir
                                                  get-mod-time
                                                  get-size]]
        [reverie.middleware :only [wrap-access]]
        [reverie.admin.templates :only [frame]]))


(defmulti row-file (fn [x & _] (:type x)))
(defmethod row-file :directory [dir qs path]
  [:tr
   [:td 
    [:a {:href (str
                (util/join-uri path (:uri dir))
                "?" qs)}
     [:i.icon-folder-close]
     (:name dir)]]
   [:td]
   [:td (get-mod-time dir)]])
(defmethod row-file :file [file qs _]
  [:tr
   [:td [:span {:uri (:uri file) :class :download} [:i.icon-download] (:name file)]]
   [:td (get-size file)]
   [:td (get-mod-time file)]])
(defmethod row-file :default [_ _ _])

(defn file-lister [files {:keys [qs up? path base-path]}]
  (let [base-path (or base-path "/admin/frame/file-picker")]
    (frame
     (-> frame-options
         (assoc :title "File picker: Images"))
     [:div#files
      [:table.table
       [:tr
        [:th "Name"] [:th "Size"] [:th "Modified"]]
       (if up?
         (row-file {:type :directory
                    :name ".."
                    :uri (str "/" (util/uri-but-last-part path))} qs base-path))
       (map #(row-file % qs base-path) files)]])))

(rev/defpage "/admin/frame/file-picker" {:middleware [[wrap-access :edit]]}
  [:get ["/images"]
   (file-lister (list-dir "images" "") {:qs (:query-string request)})]
  [:get ["/images/:path" {:path #".*"}]
   (file-lister (list-dir "images" path) {:qs (:query-string request)
                                          :up? true
                                          :path (str "images/" path)})])
