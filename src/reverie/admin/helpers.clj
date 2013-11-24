(ns reverie.admin.helpers
  (:use reverie.admin.validators
        [reverie.atoms :only [apps]])
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [clojure.string :as s]))


(defn string->sql-datetime [timestamp]
  (if (valid-datetime? timestamp)
   (let [timestamp (s/trim timestamp)
         timestamp (case (count timestamp)
                     16 (str timestamp ":00")
                     13 (str timestamp ":00:00")
                     10 (str timestamp "00:00:00")
                     timestamp)
         fmt (format/formatters :mysql)]
     (->> timestamp (format/parse fmt) coerce/to-sql-time))
   timestamp))

(defn sql-datetime->string [timestamp]
  (if (instance? java.sql.Timestamp timestamp)
    (format/unparse (format/formatters :mysql) (coerce/from-sql-time timestamp))
    timestamp))

(defn string->sql-date [timestamp]
  (if (valid-date? timestamp)
   (let [timestamp (s/trim timestamp)
         fmt (format/formatters :date)]
     (->> timestamp (format/parse fmt) coerce/to-sql-time))
   timestamp))

(defn sql-date->string [timestamp]
  (if (instance? java.sql.Date timestamp)
    (format/unparse (format/formatters :date) (coerce/from-sql-time timestamp))
    timestamp))


(defn get-app-paths [app]
  (reduce (fn [out [_ _ options _]]
            (if (nil? options)
              out
              (conj out
                    [(:app/path options)
                     (:app.path/help options)])))
          [[:* "All paths"]]
          (:fns (get @apps (keyword app)))))
