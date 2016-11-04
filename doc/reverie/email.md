# reverie.email

Email module for reverie.


## queue-message

When wanting to send emails.

```clojure
(ns some-namespace
  (:require [reverie.email :as email]))
  
;; as a map
(email/queeu-message {:from "from@example.com"
                      :to "to@example.com"
                      :subject "My subject"
                      :body "No text"})

;; as to, subject, body
(email/queue-message "to@example.com" "My subject" "No text")


;; as from, to, subject, body
(email/queue-message "from@example.com" "to@example.com" "My subject" "No text")
```


## example config in settings.edn

```clojure
{:email {:provider-settings {:from "Default From <from@server-for-example.com>"
                             :postal {:user "admin@server-for-example.com"
                                      :pass "password"
                                      :ssl true
                                      :host "smtp.server-for-example.com"}}}}
```
