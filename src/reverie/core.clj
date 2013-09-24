(ns reverie.core
  (:require [korma.core :as korma]
            [reverie.object :as o]
            [reverie.page :as p])
  (:use clout.core
        reverie.atoms
        [reverie.helper-macros :only [object-funcs request-method]]
        [reverie.util :only [generate-handler mode?]]
        [slingshot.slingshot :only [try+ throw+]]))


(defn- get-attributes [options]
  (map symbol (map name (keys (:attributes options)))))

(defn area-render [obj request]
  (o/render (assoc-in request [:reverie :object-id] (:id obj))))

(defmacro area [name]
  (let [name (keyword name)]
    `(let [~'serial (get-in ~'request [:reverie :page-serial])
           ~'request (assoc-in ~'request [:reverie :area] ~name)]
       (if (mode? ~'request :edit)
         [:div.reverie-area {:area ~name :page-serial ~'serial}
          [:div.reverie-area-holder
           [:span.reverie-area-panel (str "area " (name ~name))]]
          (map #(area-render % ~'request) (p/objects ~'request))]
         (map #(area-render % ~'request) (p/objects ~'request))))))

(defn raise-response [response]
  (throw+ {:type :ring-response :response response}))


(defmacro deftemplate [template options & body]
  (let [template (keyword template)]
    `(swap! templates assoc ~template {:options ~options
                                       :fn (fn [~'request] (try+ {:status 200
                                                                 :headers (or (:headers ~options) {})
                                                                 :body ~@body}
                                                                (catch [:type :ring-response] {:keys [~'response ~'type]}
                                                                  ~'response)))})))

(defmacro defobject [object options & methods]
   (let [object (keyword object)
         settings {}
         attributes (get-attributes options)
         table-symbol (or (:table options) object)
         body `(object-funcs ~attributes ~methods)]
     `(swap! objects assoc ~object (merge {:options ~options
                                           :entity ~table-symbol} ~body))))

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

(defn get-default-module-fns []
  (:fns (get @pages "/admin/frame/module/reverie-default-module")))

(defmacro defmodule [name options & methods]
  (let [name (keyword name)
        path (str "/admin/frame/module/" (clojure.core/name name))
        fns (if (:admin-interface? options) (get-default-module-fns) [])]
    (loop [[method & methods] methods
           fns fns]
      (if (nil? method)
        (do
          (add-route! path {:type :page :uri path})
          `(do
             (swap! modules assoc ~name ~options)
             (swap! pages assoc ~path {:options ~options :fns ~fns})))
        (recur methods (conj fns `(request-method ~method)))))))
