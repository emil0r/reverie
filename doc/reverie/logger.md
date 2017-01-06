# reverie.logger

Logger takes two parameters. Logger is dependant on [timbre](https://github.com/ptaoussanis/timbre).

- prod?, which decided whether to initiate the other appenders
- appenders, which is a map with appender options


## Example config in settings.edn

```clojure
{:log {:rotor {:path "logs/reverie.log"}
       :postal {:from "root@reverie"
                :to "user@example.com"
                :settings {:user "log@example.com"
                           :pass "password"
                           :ssl true
                           :host "smtp.example.com"}}}}
```


## Adding new appender

Add a new defmethod to the multimethod get-appender in reverie.logger. 
It takes one argument in the form of a [k v] vector from the map of appenders 
being sent in and is dispatched on the k value. Look in the code for specifics.
