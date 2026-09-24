package com.kylecorry.trail_sense.shared.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InvalidatableCachedValue<K, V : Any> {
    @Volatile
    private var cached: Entry<K, V>? = null
    private val loadLock = Mutex()
    private val invalidationLock = Any()
    private var version = 0L

    suspend fun getOrLoad(key: K, load: suspend () -> V): V {
        while (true) {
            cached?.takeIf { it.key == key }?.let { return it.value }
            val value = loadLock.withLock {
                cached?.takeIf { it.key == key }?.value ?: run {
                    val currentVersion = synchronized(invalidationLock) { version }
                    val loaded = load()
                    synchronized(invalidationLock) {
                        if (currentVersion != version) {
                            null
                        } else {
                            cached = Entry(key, loaded)
                            loaded
                        }
                    }
                }
            }
            if (value != null) return value
        }
    }

    fun invalidate() {
        synchronized(invalidationLock) {
            version++
            cached = null
        }
    }

    private data class Entry<K, V>(val key: K, val value: V)
}
