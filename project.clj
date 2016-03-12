(defproject reverie "0.7.0"
  :description "A CMS for power users. This package is deprecated. Use reverie-core and reverie-sql instead."
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-modules "0.3.10"]]
  :modules {:dirs ["reverie-core"
                   "reverie-sql"
                   "reverie-batteries"]
            :subprocess nil})
