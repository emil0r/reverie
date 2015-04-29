# downstream

Used for passing values on a per request/response cycle basis. Pratically what it means is that if you are deep down in some code path and you are executing code that has the value of the title you want to use for the page you can use downstream to pass on the value for later use.

## example

```clojure

(require '[reverie.downstream :as downstream])
(require '[reverie.page :as page])

(defn executed-first [request page properties params]
  (let [title (get-title-for-page page)]
    (downstream/assoc! :passed-down-title title)
    [:h1 title]))


(defn head-for-template-that-is-executed-later [page]
  [:head
    [:title (or (downstream/get :passed-down-title) (page/title page))]
    ;; more code...
    ])
