(ns reverie.core
  (:require [reverie.object :as o]
            [reverie.page :as p])
  (:use clout.core
        [hiccup.core :only [html]]
        reverie.atoms
        [reverie.helper-macros :only [object-funcs request-method]]
        [reverie.util :only [mode?]]
        [slingshot.slingshot :only [try+ throw+]]))


(defn- get-attributes [options]
  (map symbol (map name (keys (:attributes options)))))

(defn- ->html [parts render-fn]
  (if render-fn
    (apply render-fn parts)
    parts))

(defn area-render [obj request]
  (let [render-fn (if-not (false? (get-in @objects [(-> obj :name keyword) :options :area/use-render-fn?])) ;; negative if, default is true
                    (-> @settings :core :area/render-fn))]
   (if (mode? request :edit)
     [:div.reverie-object {:object-id (:id obj)}
      [:div.reverie-object-holder
       [:span.reverie-object-panel (str "object " (:name obj))]]
      (->html (o/render (assoc-in request [:reverie :object-id] (:id obj))) render-fn)]
     (->html (o/render (assoc-in request [:reverie :object-id] (:id obj))) render-fn))))

(defn render-with-template [request serial areas name]
  (let [out (get areas name)
        objects (p/objects request)]
    (if (mode? request :edit)
      (html [:div.reverie-area {:area name :page-serial serial}
             [:div.reverie-area-holder
              [:span.reverie-area-panel (str "area " (clojure.core/name name))]]
             (map #(area-render % request) (remove #(pos? (:order %)) objects))
             [:div.reverie-origo
              [:div.reverie-origo-holder
               [:span.reverie-origo-panel (str "origo")]]
              out]
             (map #(area-render % request) (remove #(neg? (:order %)) objects))])
      (html
       (list
        (map #(area-render % request) (remove #(pos? (:order %)) objects))
        out
        (map #(area-render % request) (remove #(neg? (:order %)) objects)))))))

(defmacro area [name]
  (let [name (keyword name)]
    `(let [~'serial (get-in ~'request [:reverie :page-serial])
           ~'request (assoc-in ~'request [:reverie :area] ~name)]
       (case (get-in ~'request [:reverie :overriden])
         :with-template (let [~'areas (get-in ~'request [:reverie :overridden/areas])]
                          (render-with-template ~'request ~'serial ~'areas ~name))
         (if (mode? ~'request :edit)
           (html [:div.reverie-area {:area ~name :page-serial ~'serial}
                  [:div.reverie-area-holder
                   [:span.reverie-area-panel (str "area " (name ~name))]]
                  (map #(area-render % ~'request) (p/objects ~'request))])
           (html (map #(area-render % ~'request) (p/objects ~'request))))))))

(defn raise-response [response]
  (throw+ {:type :ring-response :response response}))


(defmacro deftemplate [template options & body]
  (let [template (keyword template)]
    `(swap! templates assoc ~template {:options ~options
                                       :fn (fn [~'request] (try+ {:status 200
                                                                 :headers (merge
                                                                           {"Content-Type" "text/html; charset=utf-8"}
                                                                           (:headers ~options))
                                                                 :body ~@body}
                                                                (catch [:type :ring-response] {:keys [~'response ~'type]}
                                                                  ~'response)))})))

(defmacro override!
  "Override already defined objects' options and/or methods"
  [what which options & methods]
  (case what
    :object (if-let [obj (get @objects which)]
              (let [attributes (get-attributes (:options obj))
                    new-fns `(object-funcs ~attributes ~methods)]
                
                `(swap! objects assoc ~which (merge ~obj
                                                    {:options (merge (:options ~obj)
                                                                     ~options)}
                                                    ~new-fns)))
              (throw (Exception. (str "Unable to find " which))))
    (throw (Exception. (str "No implementation to handle " what)))))

(defmacro defobject [object options & methods]
   (let [object (keyword object)
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
        (recur methods (conj fns `(request-method ~options ~method)))))))


(defmacro defpage [path options & methods]
  (loop [[method & methods] methods
         fns []]
    (if (nil? method)
      (do
        (add-route! path {:type :page :uri path})
        `(swap! pages assoc ~path {:options ~options :fns ~fns}))
      (recur methods (conj fns `(request-method ~options ~method))))))

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
        (recur methods (conj fns `(request-method ~options ~method)))))))
