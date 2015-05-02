# cache

Caching in reverie is supported through a flexible caching system. Currently some hits to the database will always occur and this is a tradeoff taken so that caching can be done on a per user basis.

Any part of the cache system can be replaced with custom implementations to cover for specific needs should they arise.

## protocols

The cache manager is implemented through four protocols. They are in order

- ICacheStore
- ICacheManager
- IPruneStrategy
- ICacheKeyStrategy

### cache store
Default implementation is given in reverie.cache. Options holds the request object in the default implementation of the cache manager. Intent is to be able to pass along data that is of interest to the cache store for more advanced implementations.

- read-cache [store options key]
- write-cache [store options key data]
- delete-cache [store options key]
- clear cache [store]


### cache manager
Default implementation is given in reverie.cache. Keeps track of variations of cached pages.

- cache! [manager page request] [manager page rendered request]
- evict! [manager page]
- clear! [manager]
- lookup [manager page request]


### prune strategy
Default implementation is given in reverie.cache.sql. Will run over all pages that have a time based pruning strategy and clear the cache for those pages.

- prune! [strategy cachemananger]


### cache key strategy
Default implementation is given in reverie.cache. Will create a hash key based on serial of the page, uri of the request, query string of the request and optionally the logged in user id and a supplied function (only an option for defpage and defapp).

- get-hash-key [cache-key-strategy page request]
