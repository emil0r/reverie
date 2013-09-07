(ns reverie.entity
  (:use [korma.core :only [defentity many-to-many belongs-to]]))

(defentity role)
(defentity page)
(defentity page_attributes)
(defentity object)
(defentity app)
(defentity group)
(defentity user)

