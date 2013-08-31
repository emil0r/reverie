(ns reverie.atoms
  (:require [clj-time.core :as time])
  (:use [korma.core :only [select where order]]
        [reverie.entity :only [page]]
        [reverie.util :only [kw->str published?]]))

(defonce apps (atom {}))
(defonce modules (atom {}))
(defonce objects (atom {}))
(defonce pages (atom {}))
(defonce routes (atom {}))
(defonce settings (atom {:edits {}}))
(defonce templates (atom {}))

(defn edit! [uri user]
  (swap! settings assoc-in [:edits uri] {:time (time/now)
                                         :user user}))

(defn view! [uri]
  (swap! settings update-in [:edits] dissoc uri))

(defn edit?
  ([uri]
     (not (nil? (get-in @settings [:edits uri]))))
  ([uri user]
     (and
      (not (nil? (get-in @settings [:edits uri])))
      (= user (get-in @settings [:edits uri :user])))))


(defn get-object-entity [name]
  (:entity (get @objects (keyword name))))

(defmulti get-template class)
(defmethod get-template clojure.lang.Keyword [name]
  (get @templates name))
(defmethod get-template java.lang.String [name]
  (get @templates (keyword name)))
(defmethod get-template :default [page]
  (->> page :template keyword (get @templates)))

(defn add-route! [uri route]
  (swap! routes assoc uri route))
(defn remove-route! [uri]
  (swap! routes dissoc uri))
(defn get-route
  "Get the uri and the data for that uri"
  [uri]
  (if-let [route-data (get @routes uri)]
    [uri route-data]
    (->>
     @routes
     (filter (fn [[k v]]
               (and
                (not= (:type v) :normal)
                (re-find (re-pattern (str "^" k)) uri))))
     (sort-by first)
     reverse
     first)))
(defn update-route!
  "Update the route. This means remove old route and plug in new route"
  [new-uri {:keys [uri] :as route}]
  (remove-route! uri)
  (add-route! new-uri route))
(defn update-route-data!
  "Update the route data associated with the specific uri"
  [uri k v]
  (if-let [[uri route-data] (get-route uri)]
    (swap! routes assoc uri (assoc route-data k v))))

(defn read-routes!
  "Read in the routes in the database"
  []
  (doseq [p (select page
                    (where (and
                            {:version [>= 0]}
                            {:version [<= 1]}))
                    (order :version :ASC))]
    (swap! routes assoc (:uri p) {:type (-> p :type keyword)
                                  :uri (-> p :uri)
                                  :page-id (-> p :id)
                                  :template (-> p :template keyword)
                                  :published? (published? p)})))


(defn get-templates
  "Get a list of the templates stringified"
  []
  (map kw->str (keys @templates)))
(defn get-apps
  "Get a list of the apps stringified"
  []
  (map kw->str (keys @apps)))
(defn get-objects
  "Get a list of the objects stringified"
  []
  (map kw->str (keys @objects)))
