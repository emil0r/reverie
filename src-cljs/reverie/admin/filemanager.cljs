(ns reverie.admin.filemanager
  "This namespace is only run in the frame for the file manager"
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [ev$ activate!]]))

(def ^:private active-file (atom nil))

(defmulti get-presentation (fn [e$] (keyword (jq/attr e$ :file-type))))
(defmethod get-presentation :image [e$]
  [:tr
   [:th "Image"]
   [:img {:src (jq/attr e$ :uri) :width 200 :height 200}]])
(defmethod get-presentation :default [e$])

(defn info-window []
  (-> :#info
      jq/$
      (jq/html (crate/html [:div.drop-area "Drop files here"]))))

(defn info-file! [e]
  (.stopPropagation e)
  (let [e$ (ev$ e)]
    (activate! e$ :span.active)
    (reset! active-file e$)
    (-> :#info
        jq/$
        (jq/html (crate/html [:table.table
                              [:tr [:th "Name"] [:td (jq/attr e$ :name)]]
                              [:tr [:th "Size"] [:td (jq/attr e$ :size)]]
                              [:tr [:th "Modified"] [:td (jq/attr e$ :mod)]]
                              [:tr [:th "URI"] [:td (jq/attr e$ :uri)]]
                              (get-presentation e$)
                              [:tr [:th] [:td [:button.btn-primary.btn {:id "move"} "Move"]]]
                              [:tr [:th] [:td [:button.btn-primary.btn {:id "delete"} "Delete"]]]])))))



(defn click-delete! [e]
  (.stopPropagation e)
  (let [uri (jq/attr @active-file :uri)]
    (jq/xhr [:get (str "/admin/api/filemanager/delete/" uri)]
            nil
            (fn [data]
              (if (.-result data)
                (do
                 (-> @active-file
                     (jq/parents :tr)
                     jq/remove)
                 (info-window)))))))

(defn click-move! [e]
  (.stopPropagation e)
  (util/log (-> @active-file (jq/attr :name))))

(defn init []
  (info-window)
  (-> :span.file
      jq/$
      (jq/on :click info-file!))
  (-> :html
      jq/$
      (jq/on :click info-window))
  (-> :#info
      jq/$
      (jq/delegate :#move :click click-move!)
      (jq/delegate :#delete :click click-delete!)))
