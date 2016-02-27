# Typical structure of a project


```

-- src
   -- <project_name>
      command.clj ;; run commands from the command line
      core.clj ;; main entry point for uberjar
      dev.clj ;; development namespace
      init.clj ;; namespace for initializing all things
      -- apps
        ;; all apps should go in here and then loaded through init.clj
      -- endpoints
        ;; all endpoints should go in here and then loaded through init.clj
      -- objects
        ;; all objects should go in here and then loaded through init.clj
      -- modules
        ;; all modules should go in here and then loaded through init.clj
      -- templates
        ;; all templates should go in here and then loaded through init.clj
      
```
