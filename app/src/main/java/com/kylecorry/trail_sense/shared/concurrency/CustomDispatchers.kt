package com.kylecorry.trail_sense.shared.concurrency

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object CustomDispatchers {
    fun newFixedThreadDispatcher(
        threads: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2),
        name: String = "CustomDispatcher"
    ): ExecutorCoroutineDispatcher {
        val threadCounter = AtomicInteger(1)
        return Executors.newFixedThreadPool(threads) { runnable ->
            Thread(runnable, "$name-${threadCounter.getAndIncrement()}").apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()
    }
}
