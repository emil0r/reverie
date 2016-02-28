# system

reverie.system is the namespace that holds and manages the state for reverie. It also holds two helper functions for loading namespaces.


## functions

Here be dragons. When dealing with these you're now going into the wonderful world of state.

### storage functions

system defines an atom called storage where everything from the various macros in reverie.core is stored. These functions do not return say an actual object, but the meta data for the object as defined in the system. They are as follows:

- apps []
  - get all defined apps
- app [key]
  - get specified app
- templates []
  - get all defined templates
- template [key]
  - get specified template
- raw-pages []
  - get all raw pages. this is the internal name for endpoints
- raw-page [key]
  - get specified raw page
- objects []
  - get all objects
- object [key]
  - get specified object
- modules []
  - get all modules
- module [key]
  - get specified module
- migrations []
  - get all migrations
- migration [key]
  - get specified migration

### system functions

Helper functions for accessing some of the components specified by reverie. They are as follows. The database, filemanager and settings components are all sent along every request in the :reverie key.

 - get-db []
 - get-site []
 - get-filemanager []
 - get-cachemanager []
 - get-settings []
