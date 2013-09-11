(ns reverie.admin.filemanager
  "This namespace is only run in the frame for the file manager"
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [shoreleave.browser.storage.localstorage :as localstorage])
  (:use [reverie.util :only [ev$ activate!]]))

(def ^:private files (atom {}))


(defmulti get-presentation (fn [e$] (keyword (jq/attr e$ :file-type))))
(defmethod get-presentation :image [e$]
  [:tr
   [:th "Image"]
   [:img {:src (jq/attr e$ :uri) :width 200 :height 200}]])
(defmethod get-presentation :default [e$])

(defn get-move-button [move-file]
  (if move-file
    [:div.move-file
     [:button
      {:class "btn btn-primary"
       :id "move-file"
       :uri (:uri move-file)}
      (str "Move " (:name move-file) " here")]]))

(defn info-window []
  (let [move-file (:move-file @files)]
    (-> :#info
        jq/$
        (jq/html (crate/html
                  [:div
                   (get-move-button move-file)
                   [:div.drop-area "Drop files here"]])))))

(defn info-file [$file]
  (let [move-file (:move-file @files)]
     (-> :#info
         jq/$
         (jq/html (crate/html
                   [:div
                    (get-move-button move-file)
                    [:table.table
                     [:tr [:th "Name"] [:td (jq/attr $file :name)]]
                     [:tr [:th "Size"] [:td (jq/attr $file :size)]]
                     [:tr [:th "Modified"] [:td (jq/attr $file :mod)]]
                     [:tr [:th "URI"] [:td (jq/attr $file :uri)]]
                     (get-presentation $file)
                     [:tr [:th] [:td [:button.btn-primary.btn {:id "move"} "Move"]]]
                     [:tr [:th] [:td [:button.btn-primary.btn {:id "delete"} "Delete"]]]]])))))

(defn click-file! [e]
  (.stopPropagation e)
  (let [e$ (ev$ e)]
    (activate! e$ :span.active)
    (swap! files assoc :active e$)
    (info-file e$)))



(defn click-delete! [e]
  (.stopPropagation e)
  (let [uri (jq/attr (:active @files) :uri)]
    (jq/xhr [:post "/admin/api/filemanager/delete"]
            {:path uri}
            (fn [data]
              (if (.-result data)
                (do
                 (-> (:active @files)
                     (jq/parents :tr)
                     jq/remove)
                 (info-window)))))))

(defn click-move! [e]
  (.stopPropagation e)
  ;; (jq/xhr [:post "/admin/api/filemanager/move"]
  ;;         {:from nil :to nil}
  ;;         (fn [data]
  ;;           (if (.-result data)
  ;;             (util/log data))))
  )

(defn click-move-initiate! [e]
  (.stopPropagation e)
  (let [e$ (ev$ e)
        name (jq/attr e$ :name)
        uri (jq/attr e$ :uri)]
    (swap! files assoc :move-file {:name name
                                   :uri uri})
    (jq/xhr [:post "/admin/api/filemanager/move-initiate"]
            {:name name :uri uri}
            (fn [data]
              (if (.-result data)
                (info-file (:active @files)))))))

(defn init []
  (info-window)
  (-> :span.file
      jq/$
      (jq/on :click click-file!))
  (-> :html
      jq/$
      (jq/on :click info-window))
  (-> :#info
      jq/$
      (jq/delegate :#move :click click-move-initiate!)
      (jq/delegate :#delete :click click-delete!)))
