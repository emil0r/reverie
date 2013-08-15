(ns reverie.core
  (:require [korma.core :as korma])
  (:use clout.core
        [reverie.entity :only [object]]
        [slingshot.slingshot :only [try+ throw+]]))

(defonce apps (atom {}))
(defonce modules (atom {}))
(defonce objects (atom {}))
(defonce pages (atom {}))
(defonce routes (atom {}))
(defonce settings (atom {}))
(defonce templates (atom {}))

(defn get-object-entity [name]
  (:entity (get @objects (keyword name))))

(defn add-route! [uri route]
  (swap! routes assoc uri route))
(defn remove-route! [uri]
  (swap! routes dissoc uri))
(defn update-route! [new-uri {:keys [uri] :as route}]
  (remove-route! uri)
  (add-route! new-uri route))
(defn get-route [uri]
  (if-let [route-data (get @routes uri)]
    [uri route-data]
    (->>
     @routes
     (filter (fn [[k v]]
               (and
                (not= (:type v) :normal)
                (re-find (re-pattern k) uri))))
     first)))

(defn- get-attributes [options]
  (map symbol (map name (keys (:attributes options)))))

(defn- regex? [pattern]
  (= (class pattern) java.util.regex.Pattern))


(defn area-render [object rdata]
  ;; (object-render (get-schema object)
  ;;                (:connection rdata)
  ;;                (:db/id object)
  ;;                (assoc rdata :object-id (:db/id object)))
  )

(defmacro area [name]
  (let [name (keyword name)]
    `(let [{:keys [~'mode]} ~'rdata]
       (if (= ~'mode :edit)
         [:div.reverie-area {:id ~name :name ~name :class ~name}
          (map #(area-render % ~'rdata) (page-objects (assoc ~'rdata :area ~name)))]
         (map #(area-render % ~'rdata) (page-objects (assoc ~'rdata :area ~name)))))))

(defn raise-response [response]
  (throw+ {:type :ring-response :response response}))

(defmacro defmodule [name options]
  (let [name (keyword name)]
    `(swap! modules assoc ~name ~options )))


(defmacro deftemplate [template options & body]
  (let [template (keyword template)]
    `(swap! templates assoc ~template {:options ~options
                                       :fn (fn [~'rdata] (try+ {:status 200
                                                               :headers (or (:headers ~options) {})
                                                               :body ~@body}
                                                              (catch [:type :ring-response] {:keys [~'response ~'type]}
                                                                ~'response)))})))


(defmacro object-funcs [attributes methods & body]
  (if (every? keyword? methods)
    `(let [~'func (fn [~'data {:keys [~@attributes]}] ~@body)]
       (into {} (map vector ~methods (repeat ~'func))))
    (let [paired (into {} (map (fn [[method fn-name]] {(keyword fn-name) method}) (partition 2 methods)))
          bodies (map (fn [[fn-name & fn-body]] [(keyword fn-name) fn-body]) (filter vector? body))]
      (loop [[func-vector & r] bodies
             m {}]
        (if (nil? func-vector)
          m
          (let [[fn-name fn-body] func-vector]
            (if-let [method (paired (first func-vector))]
              (recur r (assoc m method `(fn [~'request {:keys [~@attributes]}] ~@fn-body)))
              (recur r m))))))))

(defmacro defobject [object options methods & args]
   (let [object (keyword object)
         settings {}
         attributes (get-attributes options)
         table-symbol (or (:table options) object)
         body `(object-funcs ~attributes ~methods ~@args)]
     `(swap! objects assoc ~object (merge {:options ~options
                                           :entity ~table-symbol} ~body))))

(defmacro request-method
  "Pick apart the request methods specified in other macros"
  [[method options & body]]
  (case method
    :get (let [[route _2 _3] options
               regex (if (every? regex? (vals _2)) _2 nil)
               route (if (nil? regex)
                       (route-compile route)
                       (route-compile route regex))
               method-options (if (nil? regex) _2 _3)
               keys (vec (map #(-> % name symbol) (:keys route)))
               func `(fn [~'data {:keys ~keys}] (try+ {:status 200
                                                       :headers (or (:headers ~method-options) {})
                                                       :body ~@body}
                                                      (catch [:type :ring-response] {:keys [~'response]}
                                                        ~'response)))]
           [method route method-options func])
    (let [[route _2 _3 _4] options
          [regex method-options form-data]
          (let [regex (if (and (map? _2) (every? regex? (vals _2))) _2 nil)]
            (case [(nil? regex) (nil? _3) (nil? _4)]
              [true true true] [regex nil _2]
              [true false true] [regex nil _3]
              [false false true] [regex nil _3]
              [regex _3 _4]))
          route (if (nil? regex)
                  (route-compile route)
                  (route-compile route regex))
          keys (vec (map #(-> % name symbol) (:keys route)))
          func `(fn [~'data {:keys ~keys} ~form-data] (try+ {:status 200
                                                             :headers (or (:headers ~method-options) {})
                                                             :body ~@body}
                                                            (catch [:type :ring-response] {:keys [~'response]}
                                                              ~'response)))]
      ;; (println [(nil? regex) (nil? _3) (nil? _4)])
      ;; (println _2 _3 _4)
      ;; (println route regex method-options form-data)
      [method route method-options func]
      )))

(defmacro defapp [app options & methods]
  (let [app (keyword app)]
    (loop [[method & methods] methods
           fns []]
      (if (nil? method)
        `(swap! apps assoc ~app {:options ~options :fns ~fns})
        (recur methods (conj fns `(request-method ~method)))))))


(defmacro defpage [path options & methods]
  (loop [[method & methods] methods
         fns []]
    (if (nil? method)
      (do
        (add-route! path {:type :page})
        `(swap! pages assoc ~path {:options ~options :fns ~fns}))
      (recur methods (conj fns `(request-method ~method))))))
