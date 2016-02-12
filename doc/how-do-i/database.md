# How do I use the database?

reverie uses [ez-database](https://github.com/emil0r/ez-database) for handling database queries. ez-database relies on org.clojure/java.jdbc in turn.

```clojure

;; some function which takes a request map

(ns some-namespace
  (:require [ez-database.core :as db]))

(defn myobject-or-page-or-module-or-etc-fn [request page properties params]
  (let [db (get-in request [:reverie :database])
        result (db/query db "SELECT 42")]
    result))


;; deconstruct the request map because you're badass like that
(defn alternative [{{db :database} :reverie :as request} page properties params]
  (let [result (db/query db "SELECT 42")]
    result))

```
