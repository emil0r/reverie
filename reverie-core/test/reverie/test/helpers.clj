(ns reverie.test.helpers)


(defmacro with-noir
  "Executes the body within the context of Noir's bindings"
  [& body]
  `(binding [session/*noir-session* (atom {})
             session/*noir-flash* (atom {})
             cookies/*new-cookies* (atom {})
             cookies/*cur-cookies* (atom {})]
     ~@body))
