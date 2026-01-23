package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

class CoroutineObjectPool<T : Any>(
    private val maxSize: Int,
    private val cleanup: suspend (T) -> Unit = {},
    private val factory: suspend () -> T
) {
    private val channel = Channel<T>(maxSize)
    private val created = AtomicInteger(0)

    suspend fun acquire(): T {
        channel.tryReceive().getOrNull()?.let { return it }

        if (created.incrementAndGet() <= maxSize) {
            return try {
                factory()
            } catch (e: Throwable) {
                created.decrementAndGet()
                throw e
            }
        } else {
            created.decrementAndGet()
        }

        return channel.receive()
    }

    suspend fun release(obj: T) {
        if (!channel.trySend(obj).isSuccess) {
            cleanup(obj)
            created.decrementAndGet()
        }
    }

    suspend fun close() {
        channel.close()
        for (obj in channel) {
            cleanup(obj)
            created.decrementAndGet()
        }
    }
}

suspend inline fun <T : Any, R> CoroutineObjectPool<T>.use(
    block: suspend (T) -> R
): R {
    val obj = acquire()
    try {
        return block(obj)
    } finally {
        release(obj)
    }
}
