(ns reverie.util
  (:require [clojure.string :as s]))


(defn kw->str
  "Keep the namespace for keywords"
  [string]
  (-> string str (s/replace #":" "")))

(defn published?
  "x == object/page"
  [x]
  (= 1 (:version x)))

(defn trash?
  "x == object/page"
  [x]
  (= -1 (:version x)))

(defn mode?
  "Mode in editing [:edit, :edit-other, :view]"
  [request mode]
  (= (get-in request [:reverie :mode]) mode))

(defn shorten-uri
  "shortens the uri by removing the unwanted part. Used for defpage and defapp"
  [request remove-part-of-uri]
  (-> request
      (assoc-in [:real-uri] (:uri request))
      (assoc-in [:uri] (clojure.string/replace
                        (:uri request)
                        ;; remove trailing slash IF there are more
                        ;; characters in the URI, ie, exemption for
                        ;; the first page
                        (re-pattern (s/replace remove-part-of-uri #".+/$" "")) ""))))

(defn revmap->kw
  "SQL databases can't store keywords. Do a transformation"
  [m]
  (let [w (merge m (if (:type m) {:type (-> m :type keyword)} {}))
        w (if (:app m) (merge w {:app (-> m :app keyword)}) w)
        w (if (:app_template_bindings m)
            (if-not (s/blank? (:app_template_bindings m))
              (merge w {:app_template_bindings
                        (-> m :app_template_bindings read-string)})
              (merge w {:app_template_bindings {}}))
            w)]
    w))

(defn revmap->str
  "SQL databases can't store keywords. Do a transformation"
  [m]
  (let [w (merge m (if (:type m) {:type (-> m :type kw->str)} {}))
        w (if (:app m) (merge w {:app (-> m :app kw->str)}) w)
        w (if-not (nil? (:app_template_bindings m))
            (merge w {:app_template_bindings
                      (-> m :app_template_bindings pr-str)})
            w)]
    w))


(defn which-version?
  "Which version in the system are we using? 0 for the as of yet unpublished, 1 for published, everything else is a version number in ascending order"
  [request]
  (if (get-in request [:reverie :editor?])
    0
    1))

(defn generate-handler
  "Take handlers, recursively apply to final handler"
  [handlers final-handler]
  (reduce (fn [current [new & args]]
            (apply new current args))
          final-handler
          handlers))

(defn middleware-wrap
  "Wrap middleware in options around the function"
  ([middleware f request]
     (let [new-f (generate-handler middleware (fn [req]
                                                (f req)))]
       (new-f request)))
  ([middleware f request args]
     (let [new-f (generate-handler middleware (fn [req]
                                                (f req args)))]
       (new-f request)))
  ([middleware f request args form-data]
     (let [new-f (generate-handler middleware (fn [req]
                                                (f req args form-data)))]
       (new-f request))))
(defn middleware-merge
  "Merge two or more middleware options into one"
  [& options]
  (loop [middleware []
         [option & options] options]
    (if (nil? option)
      middleware
      (if-let [wares (:middleware option)]
        (recur (apply conj middleware wares) options)
        (recur middleware options)))))

(defn join-uri
  "Join two or more fragmets of an URI together"
  [& uris]
  (loop [parts []
         [u & uris] uris]
    (if (nil? u)
      (str "/" (s/join "/" (flatten parts)))
      (recur (conj parts (remove s/blank? (s/split u #"/"))) uris))))

(defn uri-last-part
  "Take any uri and only return the last part corresponding to the page"
  [uri]
  (last (remove s/blank? (s/split uri #"/"))))

(defn uri-but-last-part
  "Take any uri and return everything but the last part corresponding to the page"
  [uri]
  (s/join "/" (butlast (remove s/blank? (s/split uri #"/")))))
