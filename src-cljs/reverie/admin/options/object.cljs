(ns reverie.admin.options.object
  "This namespace is only run in the frame for object edits"
  (:require [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [ev$ query-params params->querystring]]))



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
           "height=640,width=400,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))

(defn init []
  (util/log (-> js/document jq/$ (jq/find :form)))
  (-> js/document
      jq/$
      (jq/off "span[type=richtext]"))
  (-> js/document
      jq/$
      (jq/on :click "span[type=richtext]" click-richtext!)))
