(ns reverie.render)

(defprotocol IRender
  (render [component request] [component request sub-component]))


(extend-type nil
  IRender
  (render [this request] {:status 500
                          :body "Internal Server Error"
                          :headers {}})
  (render [this request sub-component] {:status 500
                                        :body "Internal Server Error"
                                        :headers {}}))
