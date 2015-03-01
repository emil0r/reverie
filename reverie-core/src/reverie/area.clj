(ns reverie.area
  (:require [reverie.render :as render]
            [reverie.page :as page]
            [reverie.object :as object]
            [reverie.system :as sys]
            [reverie.util :as util])
  (:import [reverie RenderException]))

(defn- get-object-menu []
  (str "<ul class='reverie-object-menu hidden'>"
       "<li action='edit'>Edit</li>"
       "<li action='delete'>Delete</li>"
       "<li class='reverie-bar'></li>"
       "<li class='move-object'>Move to »"

       "<ul class='move-object-to'>"
       "<li class='reverie-bar'></li>"
       "<li action='top'>Move to top</li>"
       "<li action='up'>Move up</li>"
       "<li action='down'>Move down</li>"
       "<li action='bottom'>Move to bottom</li>"
       "</ul>"

       "</li>"
       "</ul>"))

(defn- area-object-render [request edit? obj]
  (if edit?
    (list (str "<div class='reverie-object'"
               " object-id='"
               (object/id obj)
               "'>")
          "<div class='reverie-object-holder'>"
          "<span class='reverie-object-panel'>object "
          (object/name obj)
          (get-object-menu)
          "</span></div>"
          (render/render obj request)
          "</div>")
    (render/render obj request)))

(defn- neg-filter [area-name object]
  (and (neg? (object/order object))
       (= area-name (object/area object))))

(defn- pos-filter [area-name object]
  (and (pos? (object/order object))
       (= area-name (object/area object))))

(defn- get-objects-menu []
  (str "<ul class='reverie-objects'>"
       (apply str (->>
                   @sys/storage
                   :objects
                   (map (fn [[k _]]
                          (str "<li>" (util/kw->str k)  "</li>")))))
       "</ul>"))

(defrecord Area [name]
  render/IRender
  (render [this _]
    (throw (RenderException. "[component request] not implemented for reverie.area/Area")))
  (render [this request page]
    (if (contains? page :rendered)
      (get-in page [:rendered name])
      (let [edit? (get-in request [:reverie :edit?])
            [before after] (if edit?
                             [(list
                               (str "<div class='reverie-area' area='"
                                    (util/kw->str name)
                                    "' page-id='"
                                    (page/id page)
                                    "'>")
                               (str "<div class='reverie-area-holder'>"
                                    "<span class='reverie-area-panel'>area "
                                    (util/kw->str name)
                                    "</span>"

                                    "<ul class='reverie-area-menu hidden'>"
                                    "<li class='add-objects'>Add object"
                                    (get-objects-menu)
                                    "</li>"
                                    "</ul>"
                                    "</div>"))
                              "</div>"]
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
