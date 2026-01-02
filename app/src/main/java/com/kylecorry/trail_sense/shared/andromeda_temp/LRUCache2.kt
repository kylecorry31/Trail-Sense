package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A coroutine safe LRU cache, designed for use with IO operations
 */
class LRUCache2<K, T>(private val size: Int? = null, private val duration: Duration? = null) {

    private val values: MutableMap<K, T> = mutableMapOf()
    private var cachedAt: MutableMap<K, Instant> = mutableMapOf()
    private val mutexes = ConcurrentHashMap<K, Mutex>()

    suspend fun get(key: K): T? {
        return getLock(key).withLock {
            if (hasValidCache(key)) {
                values[key]
            } else {
                null
            }
        }
    }

    suspend fun put(key: K, value: T) {
        getLock(key).withLock {
            values[key] = value
            cachedAt[key] = Instant.now()
            removeOldest()
        }
    }

    suspend fun getOrPut(key: K, lookup: suspend () -> T): T {
        return getLock(key).withLock {
            if (hasValidCache(key)) {
                @Suppress("UNCHECKED_CAST")
                return@withLock values[key] as T
            }
            val newValue = lookup()
            values[key] = newValue
            cachedAt[key] = Instant.now()
            removeOldest()
            newValue
        }
    }

    suspend fun invalidate(key: K) {
        getLock(key).withLock {
            values.remove(key)
            cachedAt.remove(key)
            mutexes.remove(key)
        }
    }

    private fun getLock(key: K): Mutex {
        return mutexes.computeIfAbsent(key) { Mutex() }
    }

    private fun removeOldest() {
        if (size == null) {
            return
        }
        if (values.size <= size) {
            return
        }
        val oldest = cachedAt.minByOrNull { it.value }?.key ?: return
        values.remove(oldest)
        cachedAt.remove(oldest)
        mutexes.remove(oldest)
    }

    private fun hasValidCache(key: K): Boolean {
        return values.containsKey(key) && !isCacheExpired(key)
    }

    private fun isCacheExpired(key: K): Boolean {
        if (duration == null) {
            return false
        }

        val time = cachedAt[key] ?: return false

        val now = Instant.now()
        return time >= now || Duration.between(time, now) > duration
    }

}