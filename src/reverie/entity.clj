(ns reverie.entity
  (:use [korma.core :only [defentity many-to-many belongs-to]]))

(defentity role)
(defentity page)
(defentity page_attributes
  (belongs-to page))
(defentity object
  (belongs-to page))
(defentity app
  (belongs-to page))
(defentity group
  (many-to-many role :role_group))
(defentity user
  (many-to-many role :role_user)
  (many-to-many group :user_group))

