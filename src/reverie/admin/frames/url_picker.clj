(ns reverie.admin.frames.url-picker
  (:require [me.raynes.fs :as fs]
            [reverie.admin.templates :as t]
            [reverie.atoms :as atoms]
            [reverie.core :as rev]
            [reverie.response :as r]
            [reverie.util :as util])
  (:use [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.admin.frames.file-picker :only [row-file file-lister]]
        [reverie.admin.modules.filemanager :only [join-paths
                                                  list-dir]]
        [reverie.middleware :only [wrap-access]]))

(defn- list-pages []
  (sort (map first (filter (fn [[_ data]] (some #(= (:type data) %) [:normal :app]))
                           @atoms/routes))))

(rev/defpage "/admin/frame/url-picker" {:middleware [[wrap-access :edit]]}
  [:get ["/"]
   (let [value (-> request :params :value)]
     (t/frame
      (-> frame-options
          (assoc :title "URL picker"))
      [:div#tabbar
       [:div.goog-tab.goog-tab-selected {:tab :freestyle} "Free style"]
       [:div.goog-tab {:tab :file} "File"]
       [:div.goog-tab {:tab :page} "Page"]]
      
      [:div#freestyle
       (text-field :freestyle value)]
      [:div#page
       (drop-down :page-dropdown (cons ["-- Pick a URL --" ""] (list-pages)) value)]
      [:div#file
       [:iframe {:src (str "/admin/frame/url-picker/files?value=" value)}]]
      (hidden-field :value value)
      [:button {:class "btn btn-primary"} "Save"]))]
  [:get ["/files"]
   (let [value (-> request :params :value)]
     (t/frame
      frame-options
      (if (fs/exists? (join-paths "media" value))
        (file-lister (list-dir "" value) {:qs (:query-string request)
                                          :up? true
                                          :path value})
        (file-lister (list-dir "" "") {:qs (:query-string request)
                                       :up? false
                                       :path value}))))])
