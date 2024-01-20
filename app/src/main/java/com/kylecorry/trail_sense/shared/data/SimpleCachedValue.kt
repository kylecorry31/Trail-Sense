package com.kylecorry.trail_sense.shared.data

import com.kylecorry.trail_sense.shared.extensions.HashUtils

class SimpleCachedValue<T> {

    private var cachedValue: T? = null
    private var cachedHash: Int? = null
    private val lock = Any()

    fun getOrPut(vararg keys: Any?, value: () -> T): T {
        return synchronized(lock) {
            val hash = HashUtils.hash(*keys)
            if (cachedHash == null || cachedHash != hash) {
                cachedValue = value()
                cachedHash = hash
            }
            cachedValue!!
        }
    }
}