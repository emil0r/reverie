(ns reverie.admin.filemanager
  "This namespace is only run in the frame for the file manager"
  (:require [clojure.string :as s]
            [crate.core :as crate]
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

(defn get-current-path []
  (-> js/window .-location.pathname
      (s/split #"filemanager")
      last))

(defn valid-move? [file directory]
  (let [a (drop 1 (-> directory (s/split #"/") reverse))
        b (drop 2 (-> file (s/split #"/") reverse))]
    (not (= a b))))

(defn get-move-button [move-file]
  (cond
   ;; 1
   (and move-file
        (valid-move? (:uri move-file) (get-current-path)))
    [:div.move-file
     [:button
      {:class "btn btn-primary"
       :id "move-file"
       :uri (:uri move-file)}
      (str "Move " (:name move-file) " here")]]
    ;; 2
    move-file
    [:div.move-file
     [:button
      {:class "btn btn-info"}
      (str (:name move-file) " is ready to move")]]))

(defn info-window []
  (let [move-file (:move @files)]
    (-> :#info
        jq/$
        (jq/html (crate/html
                  [:div
                   (get-move-button move-file)
                   [:div.drop-area "Drop files here"]])))))

(defn info-file [$file]
  (let [move-file (:move @files)]
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
  (let [{:keys [name uri]} (:move @files)
        to (str "/"
                (s/join "/"
                        (remove
                         s/blank?
                         (s/split (get-current-path) #"/")))
                "/" name)]
    (jq/xhr [:post "/admin/api/filemanager/move"]
            {:from uri :to to}
            (fn [data]
              (if (.-result data)
                (-> js/window .-location .reload))))))

(defn click-move-initiate! [e]
  (.stopPropagation e)
  (let [$file (:active @files)
        name (jq/attr $file :name)
        uri (jq/attr $file :uri)]
    (swap! files assoc :move {:name name
                              :uri uri})
    (jq/xhr [:post "/admin/api/filemanager/move-initiate"]
            {:name name :uri uri}
            (fn [data]
              (if (.-result data)
                (info-file (:active @files)))))))

(defn init []
  (jq/xhr [:get "/admin/api/filemanager/meta"]
          nil
          (fn [data]
            (if (.-result data)
              (do
                (reset! files (:commands (js->clj data :keywordize-keys true)))
                (info-window)))))
  (-> :span.file
      jq/$
      (jq/on :click click-file!))
  (-> :html
      jq/$
      (jq/on :click info-window))
  (-> :#info
      jq/$
      (jq/delegate :#move :click click-move-initiate!)
      (jq/delegate :#delete :click click-delete!)
      (jq/delegate :#move-file :click click-move!)))
