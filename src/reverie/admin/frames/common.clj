(ns reverie.admin.frames.common)


(def frame-options {:css ["/admin/css/font-awesome.min.css"
                          "/admin/css/main.css"]
                    :js ["/admin/js/jquery-1.8.3.min.js"
                         "/admin/js/main-dev.js"
                         "/admin/js/eyespy.js"
                         "/admin/js/init.js"]})

(defn error-item [[error]]
  [:div.error error])
