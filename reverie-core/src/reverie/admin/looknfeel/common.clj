(ns reverie.admin.looknfeel.common
  (:require [clojure.set :as set]
            [reverie.downstream :as downstream]))

(defn link-css [link-name]
  [:link {:rel "stylesheet" :type "text/css"
          :href (str "/static/admin/css/" link-name)}])
(defn link-js [src-name]
  [:script {:type "text/javascript"
            :src (str "/static/admin/js/" src-name)}])
(defn inline-js [script]
  [:script {:type "text/javascript"} script])

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

(defn footer [& [{:keys [request filter-by extra-js extra-css] :as opts}]]
  (let [dev? (get-in request [:reverie :dev?])
        ;; union between downstream, dev? and filter-by/:all
        ;; downstream should mostly come from form/row
        filter-by (set/union
                   (downstream/get :inline-admin-filter-js)
                   (if dev? #{:dev})
                   (or filter-by #{:all}))]
    (list
     (map link-css (concat
                    (filter-links
                     filter-by
                     [[#{:all :meta} "../js/fancy-tree/skin-win7/ui.fancytree.min.css"]
                      [#{:richtext} "../js/tinymce/skins/lightgray/skin.ie7.min.css"]
                      [#{:editing} "jquery.simple-dtpicker.css"]])
                    extra-css))
     (map link-js (concat
                   (filter-links
                    filter-by
                    [[#{:all :base} "jquery.min.js"]
                     [#{:all :meta} "jquery-ui/jquery-ui.min.js"]
                     [#{:all :meta} "fancy-tree/jquery.fancytree-all.min.js"]
                     [#{:all :meta} "csrf.js"]
                     [#{:all :meta} "dom.js"]
                     [#{:all :meta :editing} "util.js"]
                     [#{:all :meta} "tabs.js"]
                     [#{:all :meta} "tree.js"]
                     [#{:all :meta} "objects.js"]
                     [#{:all :meta} "main.js"]
                     [#{:editing} "editing.js"]
                     [#{:editing} "jquery.simple-dtpicker.js"]
                     [#{:editing} "dtpicker.js"]
                     [#{:richtext} "tinymce/tinymce.min.js"]

                     ;; run these when developing the admin interface
                     ;; [#{:dev} "eyespy.js"]
                     ;; [#{:dev} "init.js"]
                     ])
                   extra-js))
     (map inline-js (downstream/get :inline-admin-js [])))))
