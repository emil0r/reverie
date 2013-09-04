(ns reverie.core
  (:require [korma.core :as korma]
            [reverie.object :as o]
            [reverie.page :as p])
  (:use clout.core
        reverie.atoms
        [reverie.util :only [generate-handler]]
        [slingshot.slingshot :only [try+ throw+]]))



(defn- get-attributes [options]
  (map symbol (map name (keys (:attributes options)))))

(defn- regex? [pattern]
  (= (class pattern) java.util.regex.Pattern))


(defn area-render [obj request]
  (o/render (assoc-in request [:reverie :object-serial] (:serial obj))))

(defmacro area [name]
  (let [name (keyword name)]
    `(let [~'mode (get-in ~'request [:reverie :mode])]
       (if (= ~'mode :edit)
         [:div.reverie-area {:area ~name}
          [:div.reverie-area-panel
           (str "area " (name ~name))]
          (map #(area-render % ~'request) (p/objects (assoc-in ~'request [:reverie :area] ~name)))]
         (map #(area-render % ~'request) (p/objects (assoc-in ~'request [:reverie :area] ~name)))))))

(defn raise-response [response]
  (throw+ {:type :ring-response :response response}))

(defmacro defmodule [name options]
  (let [name (keyword name)]
    `(swap! modules assoc ~name ~options )))


(defmacro deftemplate [template options & body]
  (let [template (keyword template)]
    `(swap! templates assoc ~template {:options ~options
                                       :fn (fn [~'request] (try+ {:status 200
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

(defn- clean-route
  "Cleans any route of a trailing slash in order to confirm with internal mechanics"
  [route]
  (clojure.string/replace route #"/$" ""))

(defmacro request-method
  "Pick apart the request methods specified in other macros"
  [[method options & body]]
  (case method
    :get (let [[route _2 _3] options
               route (clean-route route)
               regex (if (every? regex? (vals _2)) _2 nil)
               route (if (nil? regex)
                       (route-compile route)
                       (route-compile route regex))
               method-options (if (nil? regex) _2 _3)
               keys (vec (map #(-> % name symbol) (:keys route)))
               func `(fn [~'request {:keys ~keys}]
                        (try+ {:status 200
                               :headers (or (:headers ~method-options) {})
                               :body ~@body}
                              (catch [:type :ring-response] {:keys [~'response]}
                                ~'response)))]
           [method route method-options func])
    (let [[route _2 _3 _4] options
          route (clean-route route)
          ;; map all tree possible options to their correct name
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
          func `(fn [~'request {:keys ~keys} ~form-data]
                   (try+ {:status 200
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
        (add-route! path {:type :page :uri path})
        `(swap! pages assoc ~path {:options ~options :fns ~fns}))
      (recur methods (conj fns `(request-method ~method))))))
