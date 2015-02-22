(ns reverie.admin.looknfeel.common)


(defn head [title]
  [:head
   [:link {:rel "stylesheet" :type "text/css" :href "/static/admin/css/font-awesome.min.css"}]
   [:link {:rel "stylesheet" :type "text/css" :href "/static/admin/css/main.css"}]
   [:meta {:charset "UTF-8"}]
   [:title title]])
