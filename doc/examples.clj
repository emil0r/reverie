

;; name-of-template: name of the template
;; options: options. array that holds areas, pre- and post processing
;; body: actual body that will return to the ring handler
(deftemplate name-of-template options body)
(deftemplate main [:areas [:a :b :c] :pre nil :post nil]
  [:html
   [:head
    [:title "example"]]
   [:body
    (area :a)
    [:div "Hello world!"]]])



;; name-of-object: name of the object. can be given namespace
;; options: holds areas, attributes
;; methods: array of methods, array of just keywords will all
;; map to the & body below. the array can map to functions inside the
;; defobject area
;; & body: either vectors with function-name as first variable and
;; second variable as the body or a collection of statements that get
;; executed as one body 
(deftemplate name-of-object options methods & body)
(defobject object/text [:areas [:a :b]
                        :attributes [{:text {:db/ident :object.text/text
                                             :db/type db.type/string
                                             :db/cardinality :db.cardinality/one
                                             :db/doc "docstring"}
                                      :initial ""
                                      :input :text
                                      :name "Text"}]]
  [:any]
  [:div.text text])

;;
(defplugin name-of-plugin plugin-data)
(defpligin hotel {:schema [{:db/ident :hotel/name
                            :db/type :db.type/string
                            :db/cardinality :db.cardinality/string
                            :db/doc ""}]})
;; name-of-plugin: name of the plugin. can be given namespace
;; data: keeps schema and info on how to render the plugin in the CMS
