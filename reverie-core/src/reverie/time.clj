(ns reverie.time
  "Helper functions for clj-time and locale aware formatting"
  (:refer-clojure :exclude [format])
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [reverie.i18n :as i18n]
            [taoensso.tower :as tower]))

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
     (format (coerce value) to))
  ([value to locale-or-locale?]
     (format (coerce value) to locale-or-locale?)))

(defmethod format java.sql.Timestamp
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to))
  ([value to locale-or-locale?]
     (format (coerce value) to locale-or-locale?)))

(defmethod format java.sql.Date
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to))
  ([value to locale-or-locale?]
     (format (coerce value) to locale-or-locale?)))

(defmethod format java.lang.Long
  ([value]
     (format (coerce value) :date))
  ([value to]
     (format (coerce value) to))
  ([value to locale-or-locale?]
     (format (coerce value) to locale-or-locale?)))


(defn- get-locale [locale-or-locales]
  (tower/try-jvm-locale
   (if (sequential? locale-or-locales)
     (first locale-or-locales)
     locale-or-locales)))

(defmethod format :default
  ([value]
     (format value :date))
  ([value to]
     (let [fmt (if (string? to) (f/formatter to) (f/formatters to))]
       (f/unparse
        (f/with-locale
          fmt
          (get-locale i18n/*locale*))
        value)))
  ([value to locale-or-locale?]
     (if locale-or-locale?
       ;; use locale
       (if (true? locale-or-locale?)
         ;; use locale found in i18n
         (format value to)
         ;; while locale-or-locale? is truthy, it's not a boolean.
         ;; use the value for the locale
         (binding [i18n/*locale* locale-or-locale?]
           (format value to)))
       ;; skip locale
       (let [fmt (if (string? to) (f/formatter to) (f/formatters to))]
         (f/unparse fmt value)))))
