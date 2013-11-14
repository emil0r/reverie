(ns reverie.admin.filemanager
  "This namespace is only run in the frame for the file manager"
  (:require [clojure.string :as s]
            [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [ev$ activate! normalize uri-but-last join-uri]]))

(def ^:private files (atom {}))
(def ^:private meta-data (atom {}))


(defmulti get-presentation (fn [e$] (keyword (jq/attr e$ :file-type))))
(defmethod get-presentation :image [e$]
  [:tr
   [:th "Image"]
   (if-let [src (jq/attr e$ :img-src)]
     [:img {:src src}]
     "Unable to show image")])
(defmethod get-presentation :default [e$])

(defn get-current-path []
  (-> js/window .-location.pathname
      (s/split #"filemanager")
      last))

(defn valid-move? [file directory]
  (let [a (-> directory (s/split #"/") reverse)
        b (drop 1 (-> file (s/split #"/") reverse))]
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
      (str (:name move-file) " is ready to be moved")]]))

(defn get-directory-panel []
  [:div.directory-panel
   [:button.btn.btn-primary {:id "create-directory"} "Create directory"]
   (if (:deletable? @meta-data)
     [:button.btn.btn-danger {:id "delete-directory"} "Delete directory"])])
(defn get-progress-bar []
  [:div#progress.hidden [:div#bar]])

(defn info-window []
  (let [move-file (:move @files)
        elem (crate/html
              [:div
               (get-move-button move-file)
               [:div#drop-area "Drop files here"]
               (get-progress-bar)
               (get-directory-panel)])]
    (-> :#info
        jq/$
        (jq/html elem))))

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
                     [:tr [:th] [:td [:button.btn-primary.btn {:id "delete"} "Delete"]]]]
                    (get-directory-panel)])))))

(defn click-file! [e]
  (.stopPropagation e)
  (let [e$ (ev$ e)]
    (activate! e$ :span.active)
    (swap! files assoc :active e$)
    (info-file e$)))

(defn click-file-img! [e]
  (.stopPropagation e)
  (let [e$ (jq/parent (ev$ e))]
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
        to (join-uri (get-current-path) name)]
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

(defn click-create-directory! [e]
  (let [dir-name (.prompt js/window "Directory name", "")]
    (jq/xhr [:post "/admin/api/filemanager/create-directory"]
            {:name (normalize dir-name)
             :path (get-current-path)}
            (fn [data]
              (if (.-result data)
                (-> js/window .-location .reload))))))

(defn click-delete-directory! [e]
  (let [continue? (.confirm js/window "Really delete this directory?")]
    (if continue?
      (jq/xhr [:post "/admin/api/filemanager/delete-directory"]
              {:path (get-current-path)}
              (fn [data]
                (if (.-result data)
                  (set! (-> js/window .-location.href)
                        (join-uri
                         "/admin/frame/module/filemanager"
                         (-> (get-current-path) uri-but-last join-uri)))))))))

(defn draw-progress [percentage]
  (let [percentage (format "%.0f" (* 100 (float percentage)))
        $progress (jq/$ :#progress)]
    (jq/remove-class $progress :hidden)
    (-> $progress (jq/find :#bar) (jq/css :width (str percentage "%")))))

(defn update-drop-area! [text]
  (-> :#drop-area
      jq/$
      (jq/html text)))

(defn onload-upload [file total-size total-progress]
  (fn [e]
    (update-drop-area! (str "Uploading " (.-name file)))
    (swap! total-progress + (.-size file))
    (draw-progress (/ @total-progress @total-size))))
(defn onerror-upload [file]
  (fn [e]
    (util/log file)))
(defn onprogress-upload [total-size total-progress]
  (fn [e]
    (let [progress (+ @total-progress (.-loaded e))]
      (draw-progress (/ progress @total-size)))))
(defn onloadstart-upload [e])
(defn onloadend-upload [e]
  (update-drop-area! "Finished upload<br/>Drop files here")
  (draw-progress 1)
  (.location.reload js/window true))
(defn upload-file [file url total-size total-progress]
  (let [xhr (js/XMLHttpRequest.)
        form-data (js/FormData.)]
    (.open xhr "POST" url)
    (-> xhr .-onload (set! (onload-upload file total-size total-progress)))
    (-> xhr .-onerror (set! (onerror-upload file)))
    (-> xhr .-onprogress (set! (onprogress-upload total-size total-progress)))
    (-> xhr .-onloadstart (set! onloadstart-upload))
    (-> xhr .-onloadend (set! onloadend-upload))
    (.append form-data "file" file)
    (.append form-data "path" (get-current-path))
    (.send xhr form-data)))
(defn process-files [files]
  (let [total-size (atom 0)
        total-progress (atom 0)]
    (.forEach goog.array files
              (fn [f]
                (swap! total-size + (.-size f))))
    (.forEach goog.array files
              (fn [f]
                (upload-file f "/admin/api/filemanager/upload"
                             total-size
                             total-progress)))))

(defn drop-files! [e]
  (.preventDefault e)
  (process-files (.-originalEvent.dataTransfer.files e)))
(defn drag-over-files! [e]
  (.stopPropagation e)
  (.preventDefault e)
  (set! (.-originalEvent.dataTransfer.dropEffect e) "copy"))


(defn init []
  (jq/xhr [:post "/admin/api/filemanager/meta"]
          {:path (get-current-path)}
          (fn [data]
            (if (.-result data)
              (do
                (reset! files (:commands (js->clj data :keywordize-keys true)))
                (reset! meta-data (:meta (js->clj data :keywordize-keys true)))
                (info-window)))))
  (-> :span.file
      jq/$
      (jq/on :click click-file!))
  (-> :span.file>img
      jq/$
      (jq/on :click click-file-img!))
  (-> :html
      jq/$
      (jq/on :click info-window))
  (-> :#info
      jq/$
      (jq/delegate :#move :click click-move-initiate!)
      (jq/delegate :#delete :click click-delete!)
      (jq/delegate :#move-file :click click-move!)
      (jq/delegate :#create-directory :click click-create-directory!)
      (jq/delegate :#delete-directory :click click-delete-directory!)
      (jq/delegate :#drop-area :drop drop-files!)
      (jq/delegate :#drop-area :dragover drag-over-files!)))
