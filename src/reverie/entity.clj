(ns reverie.entity
  (:use [korma.core :only [defentity many-to-many belongs-to]]))

(defentity role)
(defentity page
  (many-to-many role :role_page))
(defentity page_attributes
  (belongs-to page))
(defentity object
  (belongs-to page))
(defentity app
  (belongs-to page))
(defentity user
  (many-to-many role :role_user))
(defentity group
  (many-to-many role :role_group))
