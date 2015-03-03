(ns reverie.sql.objects.image
  (:require [ez-image.core :as img]
            [reverie.core :refer [defobject]]))

(defn- image [request object {:keys [title alt src width height]} params]
  (let [[width height] (cond
                        (and width height) [width height]
                        width [width 99999]
                        height [99999 height]
                        :else [nil nil])]
    (if-not (or width height)
      [:img {:src src :title title :alt alt}]
      [:img {:src (img/cache src [:constrain width height])
             :title title :alt alt}])))



(defobject reverie/image
  {:table "batteries_image"
   :migration {:path "src/reverie/sql/objects/migrations/image/"
               :automatic? true}
   :properties-order [:title :alt :src :width :height]
   :properties {:title {:name "Title"
                        :type :text
                        :initial ""
                        :max 100}
                :alt {:name "Alt"
                      :type :text
                      :initial ""
                      :max 100}
                :src {:name "src"
                      :type :image
                      :initial ""
                      :max 255}
                :height {:name "Height"
                         :type :number}
                :width {:name "Width"
                        :type :number}}}
  {:any image})
