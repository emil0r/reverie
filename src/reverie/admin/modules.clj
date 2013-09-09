(ns reverie.admin.modules
  (:require [reverie.admin.modules.filemanager :as fm])
  (:use [reverie.core :only [defmodule]]))



(defmodule filemanager {:fn fm/main
                        :name "File manager"})
