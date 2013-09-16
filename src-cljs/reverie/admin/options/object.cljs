(ns reverie.admin.options.object
  "This namespace is only run in the frame for object edits"
  (:require [clojure.string :as s]
            [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [ev$ query-params params->querystring join-uri]]))



(defn click-richtext! [e]
  (let [params (query-params (-> js/window .-location .-href))
        field (-> e ev$ (jq/attr :field-name))]
    (.open js/window
           (str
            "/admin/frame/object/edit/richtext?form=form_object&field="
            field
            "&"
            (params->querystring params))
           "_blank"
           "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))

(defn click-image! [e]
  (let [field (-> e ev$ (jq/attr :field-name))
        value (-> (str "#" field) jq/$ jq/val)
        url (apply join-uri "/admin/frame/file-picker/images"
                   (butlast (rest (remove s/blank? (s/split value #"/")))))]
    (util/log url)
    (.open js/window
           (str
            url "?form=form_object&field-name=" field "&value=" value)
           "_blank"
           "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))

(defn init []
  (-> js/document
      jq/$
      (jq/off "span[type=richtext]")
      (jq/off "span[type=image]"))
  (-> js/document
      jq/$
      (jq/on :click "span[type=richtext]" click-richtext!)
      (jq/on :click "span[type=image]" click-image!)))
