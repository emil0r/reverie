(ns reverie.core
  (:require [reverie.area :as a]
            [reverie.render :as render])
  (:import [reverie AreaException]))

(defmacro area
  ([name]
     (let [name (keyword name)
           params (keys &env)]
       (if (and (some #(= 'request %) params)
                (some #(= 'page %) params))
         `(render/render (a/area ~name) ~'request ~'page nil)
         (throw (AreaException. "area assumes variables 'request' and 'page' to be present. If you wish to use other named variables send them after the name of the area like this -> (area :a req p)")))))
  ([name request page]
     (let [name (keyword name)]
       `(render/render (a/area ~name) ~request ~page nil))))
