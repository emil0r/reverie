(ns reverie.admin.frames.url-picker
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]
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
   (let [value (-> request :params :value)
         type? (cond
                (re-find #"^/[^\.]+\.[^\.]+$" value) :file
                (re-find #"^/.+$" value) :page
                :else :freestyle)]
     (t/frame
      (-> frame-options
          (assoc :title "URL picker"))
      [:div#tabbar
       [:div {:class (if (= type? :freestyle)
                       "goog-tab goog-tab-selected"
                       "goog-tab") :tab :freestyle} "Free style"]
       [:div{:class (if (= type? :file)
                       "goog-tab goog-tab-selected"
                       "goog-tab") :tab :file} "File"]
       [:div {:class (if (= type? :page)
                       "goog-tab goog-tab-selected"
                       "goog-tab") :tab :page} "Page"]]
      
      [:div#url-picker
       [:div {:class (if (= type? :freestyle) "tab" "tab hidden") :id :freestyle}
        (text-field :freestyle value)]
       [:div {:class (if (= type? :file) "tab" "hidden tab") :id :file}
        (if (re-find #"^/[^\.]+\.[^\.]+$" value)
          [:iframe {:src (str "/admin/frame/url-picker/files"
                              (s/replace value #"/([^\/]+?)\.[^\.]+$" "")
                              "?field-name=value&value=" value) :frameborder "0"}]
          [:iframe {:src (str "/admin/frame/url-picker/files?field-name=value&value="
                              value) :frameborder "0"}])]
       [:div {:class (if (= type? :page) "tab" "hidden tab") :id :page}
        (drop-down :page-dropdown (cons ["-- Pick a URL --" ""] (list-pages)) value)]
       (hidden-field :value value)
       [:button#save {:class "btn btn-primary"} "Save"]]))]
  [:get ["/files"]
   (file-lister (list-dir "" "") {:qs (:query-string request)
                                  :up? false
                                  :base-path "/admin/frame/url-picker/files"
                                  :path ""})]
  [:get ["/files/:path" {:path #".*"}]
   (file-lister (list-dir "" path) {:qs (:query-string request)
                                    :up? true
                                    :base-path "/admin/frame/url-picker/files"
                                    :path path})])
