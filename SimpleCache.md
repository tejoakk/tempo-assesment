## Code Review

You are reviewing the following code submitted as part of a task to implement an item cache in a highly concurrent application. The anticipated load includes: thousands of reads per second, hundreds of writes per second, tens of concurrent threads.
Your objective is to identify and explain the issues in the implementation that must be addressed before deploying the code to production. Please provide a clear explanation of each issue and its potential impact on production behaviour.

```kotlin
import java.util.concurrent.ConcurrentHashMap

class SimpleCache<K, V> {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val ttlMs = 60000 // 1 minute
    
    data class CacheEntry<V>(val value: V, val timestamp: Long)
    
    fun put(key: K, value: V) {
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }
    
    fun get(key: K): V? {
        val entry = cache[key]
        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
                return entry.value
            }
        }
        return null
    }
    
    fun size(): Int {
        return cache.size
    }
}
```

### Code Review

The biggest problem is in `get()`: when it finds an expired entry, it just returns
`null` and moves on - it never actually removes the entry from `cache`. Nothing else
in the class does either. So every key that's ever been `put()` sticks around forever,
dead or not. At thousands of reads/sec across a lot of distinct keys that's a slow,
steady memory leak, and eventually it's an OOM or a GC that's spending all its time
walking a huge live set for nothing. The fix is small - `cache.remove(key, entry)`
right there in the `if` block (the 2-arg overload, so we don't race-delete a newer
entry someone else just wrote).

That leak also makes `size()` kind of a lie - it counts every key ever written, not
what's actually still usable, so anyone using it for a capacity or monitoring decision
is working off bad numbers.

A few more things, roughly in order of how much I'd worry about them:

- **No cap on the number of entries.** Even once the leak above is fixed, there's
  nothing stopping a burst of unique keys from blowing up the heap. At this point I'd
  honestly just reach for something like Caffeine instead of hand-rolling this -
  `expireAfterWrite` + `maximumSize` gets you both fixes for free.
- **`System.currentTimeMillis()` for the TTL check.** It's wall-clock time, not
  monotonic, so an NTP sync or a manual clock change can throw off the `< ttlMs` math -
  entries could expire early or way late. `System.nanoTime()` is the right tool for
  measuring elapsed time.
- **No atomic get-or-compute.** With tens of threads all missing on the same key at
  once, they'll all fall through and all recompute + `put()` the same value
  independently - a classic cache stampede, and it happens right when load (and the
  need for the cache) is highest. Worth adding a `getOrPut()` backed by
  `ConcurrentHashMap.computeIfAbsent`.
- **No way to invalidate a single key.** If a write fails and needs to roll back
  whatever's cached, there's no hook for that - only `put()` over it or wait for the
  TTL to catch up.

For what it's worth, `get`/`put` themselves are fine thread-safety-wise -
`ConcurrentHashMap` handles that part on its own. The real problems here are all about
lifecycle: nothing ever really leaves this cache, and concurrent misses don't
coordinate with each other. Both are invisible in dev and show up as a slow memory
climb (or a load spike) a few weeks into production.
