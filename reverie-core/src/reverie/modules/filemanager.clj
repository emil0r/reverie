(ns reverie.modules.filemanager
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ez-image.core :as ez]
            [ez-web.uri :refer [join-uri uri-but-last-part]]
            [me.raynes.fs :as fs]
            [reverie.core :refer [defmodule]]
            [reverie.time :as time]
            [taoensso.timbre :as log]))


(defprotocol IFileMananger
  (base-dir [filemanager]))

(defrecord FileManager [base-dir media-dirs]
  component/Lifecycle
  (start [this]
    (doseq [dir media-dirs]
      (fs/mkdirs dir))
    (fs/mkdirs (str base-dir "/cache/images"))
    (ez/setup! (str base-dir "/cache/images/") "/cache/images/")
    (log/info "Starting FileManager")
    this)
  (stop [this]
    (log/info "Stopping FileManager")
    this)

  IFileMananger
  (base-dir [this]
    (.getAbsolutePath (io/file base-dir))))

(defn get-filemanager [base-dir media-dirs]
  (FileManager. base-dir media-dirs))



;; helper functions for module

(def ^:private time-display "YYYY-MM-dd HH:mm")
(def ^:private file-type-document ["pdf" "doc" "docx" "xls" "ods" "ans" "odc" "odf" "odm" "odp" "ods" "odt" "opx" "otg" "oth" "otp" "ots" "ott" "oxt" "ppt" "pps" "ppsx" "sdc" "sdd" "sdp" "sdw" "sgf" "smd" "smf" "stc" "sti" "stw" "sxc" "sxi" "sxm" "sxw" "txt" "vcg" "vor" "xlt" "xlthtml" "docm" "dotm" "dotx" "gfs" "grv" "gsa" "kfl" "mpp" "ost" "potm" "potx" "ppsm" "ppsx" "pptm" "pptx" "pub" "sldx" "xlam" "xlsb" "xlsm" "xlsx" "xltm" "xltx" "xsf" "xsn" "accdb" "accde" "accdr" "accdt" "bdr" "rtf"])
(def ^:private file-type-image ["jpg" "jpeg" "bmp" "gif" "tiff" "png"])
(def ^:private file-type-binary ["psd" "bin" "iso" "dmg" "exe" "raw" "xcf"])
(def ^:private file-type-compressed ["zip" "tar" "gz" "gzip" "rar"])
(def ^:private file-type-audio ["mp3" "ogg" "wav" "aac" "mp1" "mp2" "midi" "oga" "flac"])
(def ^:private file-type-movie ["3pg" "3gp2" "3gpp" "3gpp2" "amv" "avs" "bik" "dat" "divx" "avi" "mkv" "flv" "enc" "mpeg" "mpg" "m1v" "m2t" "m2ts" "m2v" "m4v" "mod" "mov" "moov" "movie" "mp2v" "mp4" "mpe" "mpcpl" "mtv" "ogm" "ogv" "ogx" "p64" "ppj" "prel" "prproj" "qt" "rec" "rf" "rm" "rmvb" "rts" "rtsl" "rtsp" "rv" "scr" "smi" "sol" "ssm" "tp" "trp" "ts" "vcd" "vbs" "vid" "vob" "wm" "wmv" "xvid"])
(def ^:private file-type-font ["ttf" "otf" "snf" "t2" "ttc"])

(defn join-paths [& paths]
  (->> paths
       (map (fn [path]
              (-> path
                  (str/replace #"\." "")
                  (str/split #"/"))) )
       (flatten)
       (remove str/blank?)
       (str/join "/")
       (str "/")))

(defn path-but-last
  "Take any uri and return everything but the last part corresponding to the page"
  [path]
  (->> (str/split path #"/")
       (remove str/blank?)
       (butlast)
       (str/join "/")
       (str "/")))

(defn- get-base-path [base]
  (-> fs/*cwd* (join-paths "media" (name base))))

(defn- get-name [path]
  (last (str/split path #"/")))

(defn- compare-listed
  "Compare fn for sort"
  [x y]
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
    :else (compare (:name x) (:name y))))


(defn get-mod-time [{:keys [mod]}]
  (if mod
    (time/format (time/coerce mod) time-display)))

(defn get-size [{:keys [size]}]
  (cond
   (< 1 (/ size 1073741824)) (str (format "%.2f" (float (/ size 1073741824))) " GiB")
   (< 1 (/ size 1048576)) (str (format "%.2f" (float (/ size 1048576))) " MiB")
   (< 1 (/ size 1024)) (str (format "%.2f" (float (/ size 1024))) " KiB")
   :else (str size " bytes")))

(defn get-file-type [path]
  (let [ending (-> path (str/split #"\.") last)]
    (cond
     (some #(= ending %) file-type-document) :document
     (some #(= ending %) file-type-image) :image
     (some #(= ending %) file-type-binary) :binary
     (some #(= ending %) file-type-compressed) :compressed
     (some #(= ending %) file-type-audio) :audio
     (some #(= ending %) file-type-movie) :movie
     (some #(= ending %) file-type-font) :font
     :else nil)))

(defmulti get-icon :file-type)
(defmethod get-icon :document [file]
  [:i.icon-file-text])
(defmethod get-icon :image [file]
  [:i.icon-picture])
(defmethod get-icon :binary [file]
  [:i.icon-sign-blank])
(defmethod get-icon :compressed [file]
  [:i.icon-archive])
(defmethod get-icon :audio [file]
  [:i.icon-music])
(defmethod get-icon :movie [file]
  [:i.icon-film])
(defmethod get-icon :font [file]
  [:i.icon-font])
(defmethod get-icon :default [file]
  [:i.icon-file])

(defn get-image-src [{:keys [uri] :as file}]
  (let [file-type (str/lower-case (-> uri (str/split #"\.") last))]
    (if (some #(= file-type %) ["jpg" "jpeg" "png"])
      (ez/cache (str "media" uri) [:constrain 200]))))

(defn get-path-info [file]
  (let [path (.getPath file)]
   {:type (cond
           (fs/directory? file) :directory
           (fs/file? file) :file
           :else :other)
    :mod (fs/mod-time file)
    :file-type (get-file-type path)
    :uri (-> path
             (str/replace (re-pattern
                           (-> path (str/split #"media") first)) "")
             (str/replace #"media" "")
             join-uri)
    :name (get-name path)
    :size (fs/size file)}))

(defn list-dir [base path]
  (let [base? (str/blank? base)
        base (get-base-path base)
        listed (remove
                #(or
                  (and base?
                       (.endsWith (.getPath %) "cache"))
                  (re-find #"/\." (.getPath %)))
                (-> base (join-paths path) fs/list-dir))]
    (sort compare-listed
          (map get-path-info listed))))

(defmulti row-file :type)

(defmethod row-file :directory [dir]
  [:tr
   [:td
    [:a {:href (join-uri "/admin/frame/module/filemanager" (:uri dir))}
     [:i.icon-folder-close]
     (:name dir)]]
   [:td]
   [:td (get-mod-time dir)]])

(defmethod row-file :file [file]
  (let [src (get-image-src file)]
   [:tr
    [:td [:span.file {:uri (:uri file)
                      :mod (get-mod-time file)
                      :size (get-size file)
                      :file-type (-> file :uri get-file-type)
                      :img-src src
                      :name (:name file)}
          (get-icon file)
          (:name file)
          (if src
            [:img {:src src}])]]
    [:td (get-size file)]
    [:td (get-mod-time file)]]))

(defmethod row-file :default [_ _])

(defn- file-lister [files {:keys [up? path]}]
  [:div.row.filemanager
   [:div#files.col-md-8
    [:table.table
     [:tr
      [:th "Name"] [:th "Size"] [:th "Modified"]]
     (if up?
       (row-file {:type :directory
                  :name ".."
                  :uri (str "/" (uri-but-last-part path))}))
     (map row-file files)]]
   [:div#info.col-md-4]])

(defn- browse [request module {:keys [path]}]
  (let [up? (not (str/blank? path))
        path (or path "")]
    {:nav "Filemanager"
     :content (file-lister (list-dir "" path) {:up? up?
                                               :path path})}))

(defmodule filemanager
  {:name "File manager"
   :roles #{:filemanager}
   :actions #{:view :upload :delete :move}
   :required-roles {:view #{:filemanager}
                    :upload #{:filemanager}
                    :delete #{:filemanager}
                    :move #{:filemanager}}
   :template :admin/main}
  [["/" {:get browse}]
   ["/:path" {:path #".*"} {:get browse}]])
