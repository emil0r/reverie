(ns reverie.time
  (:refer-clojure :exclude [format])
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]))

(defmulti coerce (fn [value & args] (class value)))

(defmethod coerce java.sql.Date
  ([value]
     (c/from-date value))
  ([value to]
     (coerce (c/from-date value) to)))

(defmethod coerce java.sql.Timestamp
  ([value]
     (c/from-sql-time value))
  ([value to]
     (coerce (c/from-sql-time value) to)))

(defmethod coerce java.sql.Date
  ([value]
     (c/from-sql-date value))
  ([value to]
     (coerce (c/from-sql-date value) to)))

(defmethod coerce java.lang.Long
  ([value]
     (c/from-long value))
  ([value to]
     (coerce (c/from-long value) to)))

(defmethod coerce java.lang.String
  ([value]
     (coerce value :date))
  ([value fmt]
     (let [fmt (if (string? fmt) (f/formatter fmt) (f/formatters fmt))]
       (f/parse fmt value)))
  ([value fmt to]
     (let [value (coerce value fmt)]
       (coerce value to))))

(defmethod coerce :default
  ([value]
     value)
  ([value to]
     (case to
       :date (c/to-date value)
       :timestamp (c/to-sql-time value)
       :java.sql.Date (c/to-sql-date value)
       :java.sql.Timestamp (c/to-sql-time value)
       :java.util.Date (c/to-date value)
       :epoch (c/to-epoch value)
       :long (c/to-long value)
       :joda (c/to-date-time value)
       :org.joda.time.DateTime (c/to-date-time value)
       value)))


(defmulti format (fn [value & args] (class value)))

(defmethod format java.sql.Date
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to)))

(defmethod format java.sql.Timestamp
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to)))

(defmethod format java.sql.Date
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to)))

(defmethod format java.lang.Long
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to)))


(defmethod format :default
  ([value]
     (format value :date))
  ([value to]
     (let [fmt (if (string? to) (f/formatter to) (f/formatters to))]
      (f/unparse fmt value))))
