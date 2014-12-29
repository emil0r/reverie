(defproject reverie "0.7.0-SNAPSHOT"
  :description "A sane CMS"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-sub "0.3.0"]]
  :sub ["reverie-core"
        "reverie-postgres"])
