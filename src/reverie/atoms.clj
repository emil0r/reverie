(ns reverie.atoms)

(defonce apps (atom {}))
(defonce modules (atom {}))
(defonce objects (atom {}))
(defonce pages (atom {}))
(defonce routes (atom {}))
(defonce settings (atom {}))
(defonce templates (atom {}))

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
(defn get-route [uri]
  (if-let [route-data (get @routes uri)]
    [uri route-data]
    (->>
     @routes
     (filter (fn [[k v]]
               (and
                (not= (:type v) :normal)
                (re-find (re-pattern (str "^" k)) uri))))
     (sort-by count)
     reverse
     first)))
(defn update-route! [new-uri {:keys [uri] :as route}]
  (remove-route! uri)
  (add-route! new-uri route))
(defn update-route-data! [uri k v]
  (if-let [[uri route-data] (get-route uri)]
    (swap! routes assoc uri (assoc route-data k v))))

