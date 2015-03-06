(ns reverie.sql.objects.image
  (:require [ez-image.core :as ez]
            [me.raynes.fs :as fs]
            [reverie.core :refer [defobject]]
            [reverie.modules.filemanager :as fm]))

(defn- image [request object {:keys [title alt src width height]} params]
  (let [fm (get-in request [:reverie :filemanager])
        constrain (cond
                   (and width height) [:constrain width height]
                   width [:constrain width]
                   height [:constrain height]
                   :else nil)]
    (if (fs/exists? (fm/get-abs-path fm src))
      (if constrain
        [:img {:src (ez/cache src constrain)
               :title title :alt alt}]
        [:img {:src src :title title :alt alt}]))))



(defobject reverie/image
  {:table "batteries_image"
   :migration {:path "src/reverie/sql/objects/migrations/image/"
               :automatic? true}
   :properties-order [:title :alt :src :width :height]
   :fields {:title {:name "Title"
                    :type :text
                    :initial ""
                    :max 100}
            :alt {:name "Alt"
                  :type :text
                  :initial ""
                  :max 100}
            :src {:name "Image"
                  :type :image
                  :initial ""
                  :max 255}
            :height {:name "Height"
                     :type :number}
            :width {:name "Width"
                    :type :number}}
   :sections [{:fields [:src :title :alt :height :width]}]}
  {:any image})
