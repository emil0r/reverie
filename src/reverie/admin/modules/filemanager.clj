(ns reverie.admin.modules.filemanager
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]
            [reverie.util :as util])
  (:use [clj-time.coerce :only [from-long]]
        [clj-time.format :only [unparse formatter]]
        [reverie.core :only [defmodule]]
        [hiccup core form]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.admin.templates :only [frame]]
        [reverie.middleware :only [wrap-access]]))


(def ^:private time-display (formatter "YYYY-MM-dd HH:mm"))

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

(defn join-paths [& paths]
  (s/join "/" (flatten (map (fn [p]
                              (-> p
                                  (s/replace #"\.\." "")
                                  (s/split #"/"))) paths))))

(defn list-dir [base path]
  (let [base (-> fs/*cwd* (join-paths "media" (name base)))
        listed (remove
                #(re-find #"^\." %)
                (-> base (join-paths path) fs/list-dir))]
    (sort
     compare-listed
     (reduce (fn [out k]
               (if (nil? k)
                 out
                 (let [p (join-paths base path k)]
                   (conj out
                         {:type (cond
                                 (fs/directory? p) :directory
                                 (fs/file? p) :file
                                 :else :other)
                          :mod (fs/mod-time p)
                          :url (-> p
                                   (s/replace (re-pattern
                                               (-> p (s/split #"media") first)) "")
                                   (s/replace #"media" "")
                                   util/join-uri)
                          :name (fs/name p)
                          :size (fs/size p)}))))
             []
             listed))))

(defn get-mod-time [{:keys [mod]}]
  (if mod
    (->> mod from-long (unparse time-display))))

(defn get-size [{:keys [size]}]
  (cond
   (< 1 (/ size 1073741824)) (str (format "%.2f" (float (/ size 1073741824))) " GiB")
   (< 1 (/ size 1048576)) (str (format "%.2f" (float (/ size 1048576))) " MiB")
   (< 1 (/ size 1024)) (str (format "%.2f" (float (/ size 1024))) " KiB")
   :else (str size " bytes")))


(defmodule filemanager {:name "File manager"
                        :middleware [[wrap-access :edit]]}
  [:get ["/"]
   (frame
    frame-options
    "foo")])
