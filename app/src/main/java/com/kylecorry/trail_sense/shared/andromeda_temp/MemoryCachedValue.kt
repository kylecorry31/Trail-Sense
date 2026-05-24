package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

class MemoryCachedValue<T>(
    initialValue: T? = null,
    private val duration: Duration? = null,
    private val cleanup: suspend (T) -> Unit = {}
) {

    private var value: T? = initialValue
    private var cachedAt = Instant.MIN
    private var mutex = Mutex()

    suspend fun get(): T? {
        return mutex.withLock {
            if (hasValidCache()) {
                value
            } else {
                null
            }
        }
    }

    suspend fun put(value: T) {
        mutex.withLock {
            this.value?.let { cleanup(it) }
            this.value = value
            this.cachedAt = Instant.now()
        }
    }

    suspend fun getOrPut(lookup: suspend () -> T): T {
        return mutex.withLock {
            val lastValue = value
            if (hasValidCache() && lastValue != null) {
                return@withLock lastValue
            }
            val newValue = lookup()
            value?.let { cleanup(it) }
            value = newValue
            cachedAt = Instant.now()
            newValue
        }
    }

    suspend fun reset() {
        mutex.withLock {
            value?.let { cleanup(it) }
            value = null
            cachedAt = Instant.MIN
        }
    }

    private fun hasValidCache(): Boolean {
        return value != null && !isCacheExpired()
    }

    private fun isCacheExpired(): Boolean {
        if (duration == null) {
            return false
        }

        val now = Instant.now()
        return cachedAt >= now || Duration.between(cachedAt, now) > duration
    }

}
