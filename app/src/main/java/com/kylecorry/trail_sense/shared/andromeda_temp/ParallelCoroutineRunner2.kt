package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ParallelCoroutineRunner2(maxParallel: Int = 8) {

    private val semaphore = Semaphore(maxParallel)

    suspend fun run(coroutines: List<suspend () -> Any>) = coroutineScope {
        for (coroutine in coroutines) {
            launch {
                semaphore.withPermit { coroutine() }
            }
        }
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