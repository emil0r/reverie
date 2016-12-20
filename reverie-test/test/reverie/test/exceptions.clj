(ns reverie.test.exceptions)

(do
  (compile 'reverie.AreaException)
  (compile 'reverie.CacheException)
  (compile 'reverie.DatabaseException)
  (compile 'reverie.MigrationException)
  (compile 'reverie.ModuleException)
  (compile 'reverie.ObjectException)
  (compile 'reverie.RenderException))
