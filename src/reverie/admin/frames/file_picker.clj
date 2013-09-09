(ns reverie.admin.frames.file-picker
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]
            [reverie.admin.templates :as t]
            [reverie.core :as rev]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [clj-time.coerce :only [from-long]]
        [clj-time.format :only [unparse formatter]]
        [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.middleware :only [wrap-access]]))


(defn- join-paths [& paths]
  (s/join "/" (flatten (map (fn [p]
                              (-> p
                                  (s/replace #"\.\." "")
                                  (s/split #"/"))) paths))))

(defn- compare-listed [x y]
  (cond
   (and
    (= (:type x) :directory)
    (= (:type y) :directory)) (compare (:name x) (:name y))
    (and
    (= (:type x) :directory)
    (= (:type y) :file)) -1
    (and
    (= (:type x) :file)
    (= (:type y) :directory)) 1
    :else (compare (:name x) (:name y)))
  )

(defn- list-dir [base path]
  (let [base (-> fs/*cwd* (join-paths "media" (name base)))
        listed (remove
                #(re-find #"^\." %)
                (-> base (join-paths path) fs/list-dir))]
    (sort
     compare-listed
     (reduce (fn [out k]
               (if (nil? k)
                 out
                 (let [p (join-paths base path k)]
                   (conj out
                         {:type (cond
                                 (fs/directory? p) :directory
                                 (fs/file? p) :file
                                 :else :other)
                          :mod (fs/mod-time p)
                          :url (-> p
                                   (s/replace (re-pattern
                                               (-> p (s/split #"media") first)) "")
                                   (s/replace #"media" "")
                                   util/join-uri)
                          :name (fs/name p)
                          :size (fs/size p)}))))
             []
             listed))))

(def time-display (formatter "YYYY-MM-dd HH:mm"))

(defn- get-size [size]
  (cond
   (< 1 (/ size 1073741824)) (str (format "%.2f" (float (/ size 1073741824))) " GiB")
   (< 1 (/ size 1048576)) (str (format "%.2f" (float (/ size 1048576))) " MiB")
   (< 1 (/ size 1024)) (str (format "%.2f" (float (/ size 1024))) " KiB")
   :else (str size " bytes")))

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
   [:td (->> dir :mod from-long (unparse time-display))]])
(defmethod row-file :file [file qs]
  [:tr
   [:td [:span.download {:url (:url file)} [:i.icon-download] (:name file)]]
   [:td (-> file :size get-size)]
   [:td (->> file :mod from-long (unparse time-display))]])
(defmethod row-file :default [_ _])

(defn- get-file-lister [files {:keys [qs up? path]}]
  (t/frame
   (-> frame-options
       (assoc :title "File-picker: Images")
       (assoc :css ["/admin/css/font-awesome.min.css"
                    "/admin/css/main.css"]))
   [:div.files
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
   (get-file-lister (list-dir :images "") {:qs (:query-string request)})]
  [:get ["/images/:path"]
   (get-file-lister (list-dir :images path) {:qs (:query-string request)
                                             :up? true
                                             :path (str "images/" path)})])
