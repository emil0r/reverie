(ns reverie.entity
  (:use [korma.core :only [defentity table]]))

(defentity role)
(defentity page)
(defentity page-attributes (table :page_attributes))
(defentity object)
(defentity app)
(defentity group)
(defentity user)

