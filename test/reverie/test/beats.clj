(ns reverie.test.beats
  (:require [clj-time.core :as time]
            [reverie.atoms :as atoms]
            [reverie.beats.objects :as objects]
            [reverie.beats.pages :as pages])
  (:use midje.sweet))


(swap! atoms/settings assoc-in [:edits :objects 1] {:time (time/now)
                                                    :user "emil"
                                                    :page-id 1
                                                    :action :cut})

(swap! atoms/settings assoc-in [:edits :objects 2] {:time (time/now)
                                                    :user "emil"
                                                    :page-id 1
                                                    :action :copy})


(objects/clear-cuts)
