(ns reverie.modules.filemanager
  "Filemanager and filepicker"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ez-image.core :as ez]
            [ez-web.uri :refer [join-uri uri-but-last-part]]
            [hiccup.page :refer [html5]]
            [hiccup.form :as hf]
            [me.raynes.fs :as fs]
            [reverie.admin.looknfeel.common :as common]
            [reverie.admin.looknfeel.form :as form]
            [reverie.auth :as auth]
            [reverie.core :refer [defmodule defpage]]
            [reverie.database :as db]
            [reverie.module.entity :as e]
            [reverie.time :as time]
            [reverie.util :as util]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.util.response :as response]
            [taoensso.timbre :as log]))

(defn- fs-list-dir [path]
  (map #(if (string? %) (io/file %) %) (fs/list-dir path)))

(defn- join-paths [& paths]
  (->> paths
       (map (fn [path]
              (-> path
                  (str/replace #"\.\." "")
                  (str/split #"/"))) )
       (flatten)
       (remove str/blank?)
       (str/join "/")
       (str "/")))

(defprotocol IFileMananger
  (base-dir [filemanager])
  (get-abs-path [filemanager path]))

(defrecord FileManager [base media-dirs]
  component/Lifecycle
  (start [this]
    (log/info "Starting FileManager" {:base-dir (base-dir this)
                                      :media-dirs media-dirs})
    (doseq [dir media-dirs]
      (fs/mkdirs dir))
    (fs/mkdirs (str (base-dir this) "/cache/images"))
    (ez/setup! {:save-path (str (base-dir this) "/cache/images/")
                :web-path "/cache/images/"
                :base-dir (base-dir this)})

    this)
  (stop [this]
    (log/info "Stopping FileManager")
    this)

  IFileMananger
  (base-dir [this]
    (.getAbsolutePath (io/file base)))
  (get-abs-path [this path]
    (join-paths (base-dir this) path)))


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

(defn- path-but-last
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


(defn- get-mod-time [{:keys [mod]}]
  (if mod
    (time/format (time/coerce mod) time-display)))

(defn- get-size [{:keys [size]}]
  (cond
    (< 1 (/ size 1073741824)) (str (format "%.2f" (float (/ size 1073741824))) " GiB")
    (< 1 (/ size 1048576)) (str (format "%.2f" (float (/ size 1048576))) " MiB")
    (< 1 (/ size 1024)) (str (format "%.2f" (float (/ size 1024))) " KiB")
    :else (str size " bytes")))

(defn- get-file-type [path]
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
  [:i.fa.fa-file-text])
(defmethod get-icon :image [file]
  [:i.fa.fa-picture])
(defmethod get-icon :binary [file]
  [:i.fa.fa-sign-blank])
(defmethod get-icon :compressed [file]
  [:i.fa.fa-archive])
(defmethod get-icon :audio [file]
  [:i.fa.fa-music])
(defmethod get-icon :movie [file]
  [:i.fa.fa-film])
(defmethod get-icon :font [file]
  [:i.fa.fa-font])
(defmethod get-icon :default [file]
  [:i.fa.fa-file])

(defn- get-image-src [{:keys [uri] :as file}]
  (let [file-type (str/lower-case (-> uri (str/split #"\.") last))]
    (if (some #(= file-type %) ["jpg" "jpeg" "png"])
      (try
        (ez/cache (str "media" uri) [:constrain 200])
        (catch Exception e
          (log/warn {:what ::cache-jpeg
                     :exception e
                     :msg (str e)})
          (str "media" uri))))))

(defn- get-path-info [file]
  (try
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
                (str/replace-first #"media" "")
                join-uri)
       :name (get-name path)
       :size (fs/size file)})
    (catch Exception e
      (log/error {:what ::get-path-info
                  :exception e
                  :msg (str e)})
      {:type :other
       :mod 0
       :file-type nil
       :uri ""
       :name "Faulty file/directory"
       :size 0})))

(defn- list-dir [base path]
  (let [base? (str/blank? base)
        base (get-base-path base)
        path (join-paths base path)
        listed (remove
                #(or
                  (and base?
                       (.endsWith (.getPath %) "cache"))
                  (re-find #"/\." (.getPath %)))
                (fs/list-dir path))]
    (sort compare-listed
          (map get-path-info listed))))

(defmulti row :type)

(defmethod row :directory [{:keys [qs filepicker?] :as dir}]
  (let [uri-base (if filepicker?
                   "/admin/frame/filepicker"
                   "/admin/frame/module/filemanager")]
    [:tr
     [:td
      [:a {:href (str (join-uri uri-base (:uri dir))
                      (if filepicker?
                        (str "?" (util/qsize qs))))}
       [:i.fa.fa-folder-close]
       (:name dir)]]
     [:td]
     [:td (get-mod-time dir)]]))

(defmethod row :file [{:keys [filepicker? file-name] :as file}]
  (let [src (get-image-src file)]
    [:tr
     [:td [:span {:class (if (= (:name file) file-name)
                           "file download selected"
                           "file download")
                  :uri (:uri file)
                  :path (:path file)}
           (get-icon file)
           (:name file)
           (if filepicker?
             [:i.fa.fa-download])
           (if src
             [:img {:src src}])]]
     [:td (get-size file)]
     [:td (get-mod-time file)]]))

(defmethod row :default [_])

(defn- command [name value]
  [:input.btn.btn-primary {:type :submit :name name :id name
                           :value value}])

(defn- file-lister [files {:keys [up? path filepicker? qs]}]
  (let [uri-base (if filepicker?
                   "/admin/frame/filepicker"
                   "/admin/frame/module/filemanager")]
    [:div.row.filemanager
     [:div#files.col-md-8
      [:table.table
       [:tr
        [:th "Name"] [:th "Size"] [:th "Modified"]]
       (if up?
         (row {:type :directory
               :name ".."
               :uri (uri-but-last-part path)
               :filepicker? filepicker?
               :qs qs}))
       (map #(row (assoc %
                         :file-name (get qs "file-name")
                         :filepicker? filepicker?
                         :qs qs)) files)]]
     (if-not filepicker?
       [:div#info.col-md-4
        [:div#directory-commands
         [:h3 "Directory commands"]
         (hf/form-to
          [:post ""]
          (anti-forgery-field)
          [:div (hf/text-field {:class :form-control} :dir "")]
          (command "__create-dir" "Create directory"))
         (if (empty? files)
           (hf/form-to
            [:post ""]
            (anti-forgery-field)
            (command "__delete-dir" "Delete directory")))]

        [:div#file-commands
         [:h3 "File commands"]
         (hf/form-to
          {:enctype "multipart/form-data"}
          [:post ""]
          (anti-forgery-field)
          [:div [:input {:type :file :name :upload}]]
          (command "__upload" "Upload file"))
         (hf/form-to
          [:post ""]
          (anti-forgery-field)
          (hf/hidden-field :delete "")
          (command "__delete" "Delete file"))]])]))

(defn- browse [request module {:keys [path]}]
  (let [up? (not (str/blank? path))
        path (or path "")]
    {:nav "Filemanager"
     :content (file-lister (list-dir "" path) {:up? up?
                                               :path path})
     :footer (common/footer {:filter-by #{:base}
                             :extra-js ["filemanager.js"]})}))

(defn- handle-filemanager [{:keys [uri]} module {:keys [path] :as params}]
  (cond
    (contains? params :__delete) (let [path (join-paths (get-base-path "") (:delete params))]
                                   (if (fs/exists? path)
                                     (fs/delete path))
                                   (response/redirect uri))
    (and
     (-> params :upload :filename str/blank? not)
     (contains? params :__upload)) (let [file (:upload params)]
                                     (fs/copy (:tempfile file)
                                              (join-paths (get-base-path "")
                                                          path
                                                          (:filename file)))
                                     (response/redirect uri))
     (and
      (-> params :dir str/blank? not)
      (contains? params :__create-dir)) (let [dir (:dir params)
                                              path (join-paths (get-base-path "")
                                                               path
                                                               dir)]
                                          (fs/mkdir path)
                                          (response/redirect uri))
      (contains? params :__delete-dir) (let [dir-path (join-paths (get-base-path "") path)
                                             empty-dir? (empty? (fs/list-dir dir-path))]
                                         (if empty-dir?
                                           (do (fs/delete-dir dir-path)
                                               (response/redirect (join-uri
                                                                   "/admin/frame/module/filemanager"
                                                                   (uri-but-last-part path))))
                                           (response/redirect uri)))
      :else (response/redirect uri)))


;; define module
(defmodule filemanager
  {:name "File manager"
   :roles #{:filemanager}
   :actions #{:view :upload :delete :move}
   :required-roles {:view #{:filemanager}
                    :upload #{:filemanager}
                    :delete #{:filemanager}
                    :move #{:filemanager}}
   :template :admin/main}
  [["/" {:get browse :post handle-filemanager}]
   ["/:path" {:path #".*"} {:get browse :post handle-filemanager}]])


;; define file picker
(defmethod form/row :image [entity field {:keys [form-params errors
                                                 error-field-names]
                                          :as data
                                          :or {form-params {}}}]
  ;; use form-params first, otherwise pick up the default data from
  ;; the field which should hold the initial values of the entity field
  (let [field-data (or (form-params field) (data field))
        path (str/join "/" (butlast (str/split field-data #"/")))
        file-name (last (str/split field-data #"/"))]
   [:div.row-form {:style "margin-bottom: 10px;"}
    [:table
     [:tr
      [:td (hf/label field (e/field-name entity field))]
      [:td
       (form/error-items field errors error-field-names)
       [:div.hover
        (merge {:onclick (str "window.open('/admin/frame/filepicker/"
                              path
                              "?field=" (util/kw->str field)
                              "&file-name=" file-name
                              "', '_blank', 'fullscreen=no, width=800, height=640, location=no, menubar=no'); return false;")}
               (e/field-attribs entity field))
        (if (form-params field)
          (let [src (try (ez/cache (form-params field) [:constrain 100]) (catch Exception _))]
            [:div [:img {:src src
                         :style "margin-right: 10px;"
                         :id (str (util/kw->str field) "-image")}]]))
        "Edit image..."]
       (if-not (str/blank? (form-params field))
         [:div.hover
          (merge {:onclick (format "document.getElementById('%s').value = ''; document.getElementById('%s').src = '';"
                                   (util/kw->str field)
                                   (str (util/kw->str field) "-image"))}
                 (e/field-attribs entity field))

          "Remove image..."])

       (hf/hidden-field field (form-params field))
       (form/help-text (e/field-options entity field))]]]]))

(defmethod form/row :file [entity field {:keys [form-params errors
                                                error-field-names]
                                         :or {form-params {}}}]
  (let [path (str/join "/" (butlast (str/split (form-params field) #"/")))
        file-name (last (str/split (form-params field) #"/"))]
    [:div.row-form {:style "margin-bottom: 10px;"}
     [:table
      [:tr
       [:td (hf/label field (e/field-name entity field))]
       [:td (form/error-items field errors error-field-names)
        [:div.hover
         (merge {:onclick (str "window.open('/admin/frame/filepicker/"
                               path
                               "?field=" (util/kw->str field)
                               "&file-name" file-name
                               "', '_blank', 'fullscreen=no, width=800, height=640, location=no, menubar=no'); return false;")}
                (e/field-attribs entity field))
         [:div.file {:style "margin-right: 15px;"
                     :id (str (util/kw->str field) "-descriptor")}
          (-> (form-params field)
              (str/split #"/")
              last)]
         "Edit file..."]
        (if-not (str/blank? (form-params field))
          [:div.hover
           (merge {:onclick (format "document.getElementById('%s').value = ''; document.getElementById('%s').innerHTML = '';"
                                    (util/kw->str field)
                                    (str (util/kw->str field) "-descriptor"))}
                  (e/field-attribs entity field))

           "Remove file..."])
        (hf/hidden-field field (form-params field))
        (form/help-text (e/field-options entity field))]]]]))


(defn- filepicker [{:keys [query-params]} page {:keys [path]}]
  (let [up? (not (str/blank? path))
        path (or path "")]
    {:nav "File picker"
     :content (file-lister (list-dir "" path) {:up? up?
                                               :path path
                                               :filepicker? true
                                               :qs query-params})
     :footer (map (fn [js]
                    [:script {:type "text/javascript"
                              :src (str "/static/admin/js/" js)}])
                  ["jquery.min.js"
                   "util.js"
                   "filepicker.js"])}))

(defpage "/admin/frame/filepicker" {:template :admin/main}
  [["/" {:get filepicker}]
   ["/:path" {:path #".*"} {:get filepicker}]])
