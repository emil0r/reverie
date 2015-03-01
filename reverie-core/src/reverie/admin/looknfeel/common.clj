(ns reverie.admin.looknfeel.common)

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


(defn footer [& [{:keys [request] :as opts}]]
  (let [dev? (get-in request [:reverie :dev?])]
    (when dev?
      (list
       (map link-css ["../js/fancy-tree/skin-win7/ui.fancytree.min.css"])
       (map link-js ["jquery-2.1.3.min.js"
                     "jquery-ui/jquery-ui.min.js"
                     "fancy-tree/jquery.fancytree-all.min.js"
                     "csrf.js"
                     "dom.js"
                     "util.js"
                     "tabs.js"
                     "tree.js"
                     "objects.js"
                     "main.js"
                     "eyespy.js"
                     "init.js"])))))
