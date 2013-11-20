(ns reverie.settings
  "Namespace for easier handling of reverie settings. Can be directly accessed through @reverie.atoms/settings as well"
  (:refer-clojure :exclude [read write])
  (:use [reverie.atoms :only [settings objects pages apps templates modules]]))


(defn read
  "Read from @settings in reverie.atoms"
  [& path]
  (get-in @settings path))

(defn write!
  "Write to @settings in reverie.atoms"
  [& path]
  (swap! settings assoc-in (butlast path) (last path)))

(defn delete!
  "Delete from @settings in reverie.atoms"
  [& path]
  (swap! settings update-in (butlast path) dissoc (last path)))


(defn options-read
  "Read options in objects, pages or apps [:object :name-of-object :path :to :value]"
  [what which & path]
  (case what
    :object (get-in @objects (flatten [which :options path]))
    :page (get-in @pages (flatten [which :options path]))
    :app (get-in @apps (flatten [which :options path]))
    :template (get-in @templates (flatten [which :options path]))
    :module (get-in @modules (flatten [which :options path]))))

(defn options-write!
  "Write options to objects, pages or apps [:object :name-of-object :path :to :value]"
  [what which & path]
  (case what
    :object (swap! objects assoc-in (flatten [which :options (butlast path)]) (last path))
    :page (swap! pages assoc-in (flatten [which :options (butlast path)]) (last path))
    :app (swap! apps assoc-in (flatten [which :options (butlast path)]) (last path))
    :template (swap! templates assoc-in (flatten [which :options (butlast path)]) (last path))
    :module (swap! modules assoc-in (flatten [which :options (butlast path)]) (last path))))

(defn options-delete!
  "Delete options from objects, pages or apps [:object :name-of-object :path :to :value]"
  [what which & path]
  (case what
    :object (swap! objects update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :page (swap! pages update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :app (swap! apps update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :template (swap! templates update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))
    :module (swap! modules update-in (remove nil? (flatten [which :options (butlast path)])) dissoc (last path))))
