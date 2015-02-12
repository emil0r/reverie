(ns reverie.area
  (:require [reverie.render :as render]
            [reverie.page :as page]
            [reverie.object :as object])
  (:import [reverie RenderException]))

(defn- area-object-render [request edit? obj]
  (if edit?
    (list (str "<div class='reverie-object'"
               " object-id='" (object/id obj) "'>")
          (render/render obj request)
          "</div>")
    (render/render obj request)))

(defn- neg-filter [area-name object]
  (and (neg? (object/order object))
       (= area-name (object/area object))))

(defn- pos-filter [area-name object]
  (and (pos? (object/order object))
       (= area-name (object/area object))))

(defrecord Area [name]
  render/IRender
  (render [this _]
    (throw (RenderException. "[component request] not implemented for reverie.area/Area")))
  (render [this request page]
    (if (contains? page :rendered)
      (get-in page [:rendered name])
      (let [edit? (= :edit (get-in request [:reverie :mode]))
            [before after] (if edit?
                             [(str "<div class='reverie-area' area='" (name name) "' page-id='" (page/id page) "'>") "</div>"]
                             [nil nil])
            middle (if (page/type? page :app)
                     (get (:rendered page) name)
                     nil)]
        (list
         before
         (map (partial area-object-render request edit?)
              (filter (partial neg-filter name) (page/objects page)))
         middle
         (map (partial area-object-render request edit?)
              (filter (partial pos-filter name) (page/objects page)))
         after)))))


(defn area [name]
  (Area. name))
