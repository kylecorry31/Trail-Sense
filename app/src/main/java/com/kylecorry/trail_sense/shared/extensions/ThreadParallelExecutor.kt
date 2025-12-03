package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.shared.Executor
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class ThreadParallelExecutor(
    private val threads: Int = Runtime.getRuntime().availableProcessors()
) :
    Executor {

    override fun <T> map(tasks: List<() -> T>): List<T> {
        if (tasks.isEmpty()) {
            return emptyList()
        }

        val executor = Executors.newFixedThreadPool(threads)
        try {
            val futures = tasks.map { task ->
                executor.submit(Callable { task() })
            }
            return futures.map { it.get() }
        } finally {
            executor.shutdown()
        }
    }

    override fun run(tasks: List<() -> Unit>) {
        if (tasks.isEmpty()) {
            return
        }

        val executor = Executors.newFixedThreadPool(threads)
        try {
            val futures = tasks.map { task ->
                executor.submit { task() }
            }
            futures.forEach { it.get() }
        } finally {
            executor.shutdown()
        }
    }
}