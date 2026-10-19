package io.github.halilozel1903.openf1

/**
 * Stores response bodies by url. Data of finished sessions never changes, so caching it is safe; requests for
 * `latest` meetings or sessions are never cached.
 */
public interface OpenF1Cache {
    public suspend fun get(url: String): String?
    public suspend fun put(url: String, body: String)
    public suspend fun clear()

    public companion object {
        /** An in-memory cache of the [maxEntries] most recently used responses. */
        public fun memory(maxEntries: Int = 64): OpenF1Cache = MemoryCache(maxEntries)
    }
}

private class MemoryCache(private val maxEntries: Int) : OpenF1Cache {
    init {
        require(maxEntries > 0) { "maxEntries must be positive, was $maxEntries" }
    }

    private val lock = Any()
    private val entries = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > maxEntries
    }

    override suspend fun get(url: String): String? = synchronized(lock) { entries[url] }
    override suspend fun put(url: String, body: String) {
        synchronized(lock) { entries[url] = body }
    }
    override suspend fun clear() {
        synchronized(lock) { entries.clear() }
    }
}
