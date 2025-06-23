package com.kylecorry.trail_sense.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ParallelCoroutineRunner(maxParallel: Int = 8) {

    private val semaphore = Semaphore(maxParallel)
    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

    suspend fun run(coroutines: List<suspend () -> Any>) {
        val jobs = mutableListOf<Job>()

        for (coroutine in coroutines) {
            jobs.add(coroutineScope.launch {
                semaphore.withPermit { coroutine() }
            })
        }

        jobs.forEach { it.join() }
    }

    suspend fun <R> run(items: List<R>, coroutine: suspend (R) -> Unit) {
        run(items.map { { coroutine(it) } })
    }

    suspend fun <T> map(coroutines: List<suspend () -> T>): List<T> {
        val items = mutableListOf<Pair<Int, T>>()
        val lock = Any()

        run(coroutines.mapIndexed { index, coroutine ->
            {
                val item = coroutine()
                synchronized(lock) {
                    items.add(index to item)
                }
            }
        })

        return items.sortedBy { it.first }.map { it.second }
    }

    suspend fun <T> mapFunctions(functions: List<() -> T>): List<T> {
        return map(functions.map { { it() } })
    }

    suspend fun <R, T> map(items: List<R>, coroutine: suspend (R) -> T): List<T> {
        return map(items.map { { coroutine(it) } })
    }

}