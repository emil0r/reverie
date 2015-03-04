(ns reverie.admin.looknfeel.common
  (:require [clojure.set :as set]))

(defn link-css [link-name]
  [:link {:rel "stylesheet" :type "text/css"
          :href (str "/static/admin/css/" link-name)}])
(defn link-js [src-name]
  [:script {:type "text/javascript"
            :src (str "/static/admin/js/" src-name)}])


(defn head [title & [{:keys [request] :as opts}]]
  [:head
   (map link-css ["font-awesome.min.css"
                  "main.css"])
   [:meta {:charset "UTF-8"}]
   [:title title]])

(defn filter-links [filter-by links]
  (->> links
       (filter (fn [[req _]]
                 (not (empty? (set/intersection req filter-by)))))
       (map (fn [[_ js]]
              js))))

(defn footer [& [{:keys [request filter-by] :as opts}]]
  (let [dev? (get-in request [:reverie :dev?])
        filter-by (set/union
                   (if dev? #{:dev})
                   (or filter-by #{:all}))]
    (list
     (map link-css (filter-links
                    filter-by
                    [[#{:all :meta} "../js/fancy-tree/skin-win7/ui.fancytree.min.css"]
                     [#{:richtext} "../js/tinymce/skins/lightgray/skin.ie7.min.css"]]))
     (map link-js (filter-links
                   filter-by
                   [[#{:all :base} "jquery-2.1.3.min.js"]
                        [#{:all :meta} "jquery-ui/jquery-ui.min.js"]
                        [#{:all :meta} "fancy-tree/jquery.fancytree-all.min.js"]
                        [#{:all :meta} "csrf.js"]
                        [#{:all :meta} "dom.js"]
                        [#{:all :meta} "util.js"]
                        [#{:all :meta} "tabs.js"]
                        [#{:all :meta} "tree.js"]
                        [#{:all :meta} "objects.js"]
                        [#{:all :meta} "main.js"]
                        [#{:richtext} "tinymce/tinymce.min.js"]
                        [#{:all :dev} "eyespy.js"]
                        [#{:all :dev} "init.js"]])))))