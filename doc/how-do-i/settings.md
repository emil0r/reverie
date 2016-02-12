# How do I use the settings?

Each reverie project has a settings.edn file which holds all the settings.

```clojure

;; some function which takes a request map

(ns some-namespace
  (:require [reverie.settings :as settings]]))


;; deconstruct the request map because you're badass like that
(defn alternative [{{settings :settings} :reverie :as request} page properties params]
  (let [info (settings/get settings [:path :to :info] "default if nothing is found")]
    info))

```

## Example settings.edn

Anything you wish to be specified on a site by site basis should go into settings.edn.

```clojure

{
 ;; reverie
 :server-mode :dev
 :server {:options {:port 3000}
          :middleware {}}
 :db {:specs {:default {:classname "org.postgresql.Driver"
                        :subprotocol "postgresql"
                        :subname "//localhost:5432/testsite"
                        :user "user"
                        :password "password"}}}

 :admin {:tasks {:edits {:minutes 30}}}
 :log {:rotor {:path "logs/test.log"}}
 :site {:host-names []}
 :filemanager {:base-dir "media"
               :media-dirs ["media/images"
                            "media/files"]}}

```


## Functions available

| Function | What does it do? |
| --- | --- |
| (true? [settings path expected]) | Is this setting really set this way? |
| (prod? [settings]) | Are we in production mode? |
| (dev? [settings]) | Are we in dev mode? |
| (initialized? [settings]) | Are settings initialized? |
| (get [settings path] | Get the settings at the specified path |
| (get [settings path default] | Get the settings at the specified path. If default is not set to nil and the value returned is nil an exception is thrown, otherwise default is returned. |
