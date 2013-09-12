(ns reverie.admin.modules.filemanager
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]
            [reverie.auth.user :as user]
            [reverie.util :as util])
  (:use [clj-time.coerce :only [from-long]]
        [clj-time.format :only [unparse formatter]]
        [reverie.core :only [defmodule defpage]]
        [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.admin.templates :only [frame]]
        [reverie.middleware :only [wrap-access]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))

(def commands (atom {}))

(defn join-paths [& paths]
  (str "/"
       (s/join "/" (remove s/blank?
                           (flatten 
                            (map (fn [p]
                                   (-> p
                                       (s/replace #"\.\." "")
                                       (s/split #"/"))) paths))))))

(defn path-but-last
  "Take any uri and return everything but the last part corresponding to the page"
  [path]
  (str "/" (s/join "/" (butlast (remove s/blank? (s/split path #"/"))))))




(def ^:private time-display (formatter "YYYY-MM-dd HH:mm"))
(def ^:private file-type-document ["pdf" "doc" "docx" "xls" "ods" "ans" "odc" "odf" "odm" "odp" "ods" "odt" "opx" "otg" "oth" "otp" "ots" "ott" "oxt" "ppt" "pps" "ppsx" "sdc" "sdd" "sdp" "sdw" "sgf" "smd" "smf" "stc" "sti" "stw" "sxc" "sxi" "sxm" "sxw" "txt" "vcg" "vor" "xlt" "xlthtml" "docm" "dotm" "dotx" "gfs" "grv" "gsa" "kfl" "mpp" "ost" "potm" "potx" "ppsm" "ppsx" "pptm" "pptx" "pub" "sldx" "xlam" "xlsb" "xlsm" "xlsx" "xltm" "xltx" "xsf" "xsn" "accdb" "accde" "accdr" "accdt" "bdr" "rtf"])
(def ^:private file-type-image ["jpg" "jpeg" "bmp" "gif" "tiff" "png"])
(def ^:private file-type-binary ["psd" "bin" "iso" "dmg" "exe" "raw" "xcf"])
(def ^:private file-type-compressed ["zip" "tar" "gz" "gzip" "rar"])
(def ^:private file-type-audio ["mp3" "ogg" "wav" "aac" "mp1" "mp2" "midi" "oga" "flac"])
(def ^:private file-type-movie ["3pg" "3gp2" "3gpp" "3gpp2" "amv" "avs" "bik" "dat" "divx" "avi" "mkv" "flv" "enc" "mpeg" "mpg" "m1v" "m2t" "m2ts" "m2v" "m4v" "mod" "mov" "moov" "movie" "mp2v" "mp4" "mpe" "mpcpl" "mtv" "ogm" "ogv" "ogx" "p64" "ppj" "prel" "prproj" "qt" "rec" "rf" "rm" "rmvb" "rts" "rtsl" "rtsp" "rv" "scr" "smi" "sol" "ssm" "tp" "trp" "ts" "vcd" "vbs" "vid" "vob" "wm" "wmv" "xvid"])
(def ^:private file-type-font ["ttf" "otf" "snf" "t2" "ttc"])

(defn- compare-listed [x y]
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

(defn- get-name [path]
  (last (s/split path #"/")))

(defn- get-base-path [base]
  (-> fs/*cwd* (join-paths "media" (name base))))

(defn get-mod-time [{:keys [mod]}]
  (if mod
    (->> mod from-long (unparse time-display))))

(defn get-size [{:keys [size]}]
  (cond
   (< 1 (/ size 1073741824)) (str (format "%.2f" (float (/ size 1073741824))) " GiB")
   (< 1 (/ size 1048576)) (str (format "%.2f" (float (/ size 1048576))) " MiB")
   (< 1 (/ size 1024)) (str (format "%.2f" (float (/ size 1024))) " KiB")
   :else (str size " bytes")))

(defn get-file-type [path]
  (if (fs/directory? path)
    nil
    (let [ending (-> path (s/split #"\.") last)]
      (cond
       (some #(= ending %) file-type-document) :document
       (some #(= ending %) file-type-image) :image
       (some #(= ending %) file-type-binary) :binary
       (some #(= ending %) file-type-compressed) :compressed
       (some #(= ending %) file-type-audio) :audio
       (some #(= ending %) file-type-movie) :movie
       (some #(= ending %) file-type-font) :font
       :else nil))))

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


(defn get-path-info [p]
  {:type (cond
          (fs/directory? p) :directory
          (fs/file? p) :file
          :else :other)
   :mod (fs/mod-time p)
   :file-type (get-file-type p)
   :uri (-> p
            (s/replace (re-pattern
                        (-> p (s/split #"media") first)) "")
            (s/replace #"media" "")
            util/join-uri)
   :name (get-name p)
   :size (fs/size p)})

(defn list-dir [base path]
  (let [base (get-base-path base)
        listed (remove
                #(re-find #"^\." %)
                (-> base (join-paths path) fs/list-dir))]
    (sort
     compare-listed
     (reduce (fn [out k]
               (if (nil? k)
                 out
                 (let [p (join-paths base path k)]
                   (conj out (get-path-info p)))))
             []
             listed))))


(defmulti row-file :type)
(defmethod row-file :directory [dir]
  [:tr
   [:td 
    [:a {:href (util/join-uri "/admin/frame/module/filemanager" (:uri dir))}
     [:i.icon-folder-close]
     (:name dir)]]
   [:td]
   [:td (get-mod-time dir)]])
(defmethod row-file :file [file]
  [:tr
   [:td [:span.file {:uri (:uri file)
                     :mod (get-mod-time file)
                     :size (get-size file)
                     :file-type (-> file :uri get-file-type)
                     :name (:name file)} (get-icon file) (:name file)]]
   [:td (get-size file)]
   [:td (get-mod-time file)]])
(defmethod row-file :default [_ _])

(defn- file-lister [files {:keys [up? path]}]
  (frame
   (-> frame-options
       (assoc :title "File manager")
       (assoc :css ["/admin/css/font-awesome.min.css"
                    "/admin/css/main.css"]))
   [:div.row.filemanager
    [:div#files.col-md-8
     [:table.table
      [:tr
       [:th "Name"] [:th "Size"] [:th "Modified"]]
      (if up?
        (row-file {:type :directory
                   :name ".."
                   :uri (str "/" (util/uri-but-last-part path))}))
      (map row-file files)]]
    [:div#info.col-md-4]]))

(defmodule filemanager {:name "File manager"
                        :middleware [[wrap-access :edit]]}
  [:get ["/"]
   (frame
    frame-options
    (file-lister (list-dir "" "") {}))]
  [:get ["/:path" {:path #".*"}]
   (file-lister (list-dir "" path) {:up? true
                                    :path path})])


(defn- legal? [path]
  (nil? (re-find #"\.\.|\/\." path)))

(defpage "/admin/api/filemanager" {:middleware [[wrap-json-params]
                                                [wrap-json-response]
                                                [wrap-access :edit]]}
  [:post ["/delete" {:keys [path]}]
   (let [p (-> path get-base-path join-paths)]
     (if (fs/exists? p)
       {:result (fs/delete p)}
       {:result false}))]
  [:post ["/move" {:keys [from to]}]
   (let [u (user/get)
         from (join-paths fs/*cwd* "media" from)
         to (join-paths fs/*cwd* "media" to)]
     (swap! commands assoc-in [u :move] nil)
     (if (and
          (legal? from)
          (legal? to)
          (fs/exists? from)
          (fs/exists? (path-but-last to)))
       {:result (and
                 (fs/copy from to)
                 (fs/delete from))}
       {:result :false :reason (cond
                                (not (legal? from)) "From path was not absolute"
                                (not (legal? to)) "To path was not absolute"
                                (not (fs/exists? from)) "From path does not exist"
                                (not (fs/exists? to)) "To path does not exist"
                                :else "Illegal operation")}))]
  [:post ["/move-initiate" {:keys [name uri]}]
   (let [u (user/get)]
     (swap! commands assoc-in [u :move] {:name name :uri uri})
     {:result true})]
  [:post ["/create-directory" {:keys [name path]}]
   (let [path (join-paths fs/*cwd* "media" path name)]
     {:result (fs/mkdir path)})]
  [:post ["/delete-directory" {:keys [path]}]
   (let [path (join-paths fs/*cwd* "media" path)]
     {:result (if (empty? (fs/list-dir path))
                (fs/delete-dir path)
                false)})]
  [:post ["/upload" {:keys [file path]}]
   (let [path (join-paths fs/*cwd* "media" path (:filename file))]
     {:result (fs/copy (:tempfile file) path)})]
  [:post ["/meta" {:keys [path]}]
   (let [u (user/get)
         media-path (join-paths fs/*cwd* "media" path)]
     {:result true
      :commands (get @commands u)
      :meta {:deletable? (and
                          (not (empty? path))
                          (empty? (fs/list-dir media-path)))}})])
