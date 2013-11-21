(ns reverie.settings
  "Namespace for easier handling of reverie settings. Can be directly accessed through @reverie.atoms/settings as well"
  (:refer-clojure :exclude [read true?])
  (:use [reverie.atoms :only [settings objects pages apps templates modules]]))


(defn true?
  "Check truthiness from @settings in reverie.atoms"
  [path expected]
  (= expected (get-in @settings path)))

(defn read
  "Read from @settings in reverie.atoms"
  [path & [default]]
  (get-in @settings path default))

(defn write!
  "Write to @settings in reverie.atoms"
  [path value]
  (swap! settings assoc-in path value))

(defn delete!
  "Delete from @settings in reverie.atoms"
  [path]
  (swap! settings update-in (butlast path) dissoc (last path)))


(defn option-true?
  "Check truthiness of option in objects, pages or apps [:object :name-of-object [:path :to :value] expected]"
  [what which path expected]
  (case what
    :object (= expected (get-in @objects (flatten [which :options path])))
    :page (= expected (get-in @pages (flatten [which :options path])))
    :app (= expected (get-in @apps (flatten [which :options path])))
    :template (= expected (get-in @templates (flatten [which :options path])))
    :module (= expected (get-in @modules (flatten [which :options path])))))

(defn option-read
  "Read options in objects, pages or apps [:object :name-of-object [:path :to :value] :default?]"
  [what which path & [default]]
  (case what
    :object (get-in @objects (flatten [which :options path]) default)
    :page (get-in @pages (flatten [which :options path]) default)
    :app (get-in @apps (flatten [which :options path]) default)
    :template (get-in @templates (flatten [which :options path]) default)
    :module (get-in @modules (flatten [which :options path]) default)))

(defn option-write!
  "Write options to objects, pages or apps [:object :name-of-object [:path :to] :value]"
  [what which path value]
  (case what
    :object (swap! objects assoc-in (flatten [which :options path]) value)
    :page (swap! pages assoc-in (flatten [which :options path]) value)
    :app (swap! apps assoc-in (flatten [which :options path]) value)
    :template (swap! templates assoc-in (flatten [which :options path]) value)
    :module (swap! modules assoc-in (flatten [which :options path]) value)))

(defn option-delete!
  "Delete options from objects, pages or apps [:object :name-of-object [:path :to :value]]"
  [what which path]
  (case what
    :object (swap! objects update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :page (swap! pages update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :app (swap! apps update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :template (swap! templates update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :module (swap! modules update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))))
