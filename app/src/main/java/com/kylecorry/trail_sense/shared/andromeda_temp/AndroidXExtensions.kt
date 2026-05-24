package com.kylecorry.trail_sense.shared.andromeda_temp

import androidx.collection.LruCache
import com.kylecorry.trail_sense.shared.concurrency.Lock
import com.kylecorry.trail_sense.shared.concurrency.NoLock

fun <K : Any, V : Any> LruCache<K, V>.getOrPut(key: K, lock: Lock = NoLock(), producer: () -> V): V {
    return lock.withLock(key) {
        get(key) ?: producer().also { put(key, it) }
    }
}

fun <K : Any, V : Any> LruCache<K, V>.getOrPutUnlockedProducer(
    key: K,
    lock: Lock,
    producer: () -> V
): V {
    lock.withLock(key) { get(key) }?.let { return it }

    val value = producer()

    return lock.withLock(key) {
        get(key) ?: value.also {
            put(key, it)
        }
    }
}
