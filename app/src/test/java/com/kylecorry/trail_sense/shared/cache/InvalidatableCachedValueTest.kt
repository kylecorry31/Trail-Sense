package com.kylecorry.trail_sense.shared.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class InvalidatableCachedValueTest {

    @Test
    fun reusesValueUntilKeyChangesOrInvalidated() = runBlocking {
        val cache = InvalidatableCachedValue<Int, Int>()
        val loads = AtomicInteger()

        assertEquals(1, cache.getOrLoad(1) { loads.incrementAndGet() })
        assertEquals(1, cache.getOrLoad(1) { loads.incrementAndGet() })
        assertEquals(2, cache.getOrLoad(2) { loads.incrementAndGet() })
        cache.invalidate()
        assertEquals(3, cache.getOrLoad(2) { loads.incrementAndGet() })
    }

    @Test
    fun retriesLoadWhenInvalidatedDuringLoad() = runBlocking {
        val cache = InvalidatableCachedValue<Int, Int>()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val loads = AtomicInteger()

        val result = async(Dispatchers.Default) {
            cache.getOrLoad(1) {
                val value = loads.incrementAndGet()
                if (value == 1) {
                    started.complete(Unit)
                    release.await()
                }
                value
            }
        }

        started.await()
        cache.invalidate()
        release.complete(Unit)
        assertEquals(2, result.await())
        assertEquals(2, cache.getOrLoad(1) { loads.incrementAndGet() })
    }
}
