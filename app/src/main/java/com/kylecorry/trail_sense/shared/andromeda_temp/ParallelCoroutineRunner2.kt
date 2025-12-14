package com.kylecorry.trail_sense.shared.andromeda_temp

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object Parallel {

    fun getProcessorCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    private fun getDefaultMaxParallel(): Int {
        return getProcessorCount().coerceAtLeast(2)
    }

    suspend fun forEach(
        coroutines: List<suspend () -> Any>,
        maxParallel: Int = getDefaultMaxParallel()
    ) =
        coroutineScope {
            val semaphore = Semaphore(maxParallel)
            for (coroutine in coroutines) {
                launch {
                    semaphore.withPermit { coroutine() }
                }
            }
        }

    suspend fun <R> forEach(
        items: List<R>,
        maxParallel: Int = getDefaultMaxParallel(),
        coroutine: suspend (R) -> Unit
    ) {
        forEach(items.map { { coroutine(it) } }, maxParallel)
    }

    suspend fun <T> map(
        coroutines: List<suspend () -> T>,
        maxParallel: Int = getDefaultMaxParallel()
    ): List<T> {
        val items = mutableListOf<Pair<Int, T>>()
        val lock = Any()

        forEach(coroutines.mapIndexed { index, coroutine ->
            {
                val item = coroutine()
                synchronized(lock) {
                    items.add(index to item)
                }
            }
        }, maxParallel)

        return items.sortedBy { it.first }.map { it.second }
    }

    suspend fun <T> mapFunctions(
        functions: List<() -> T>,
        maxParallel: Int = getDefaultMaxParallel()
    ): List<T> {
        return map(functions.map { { it() } }, maxParallel)
    }

    suspend fun <R, T> map(
        items: List<R>,
        maxParallel: Int = getDefaultMaxParallel(),
        coroutine: suspend (R) -> T
    ): List<T> {
        return map(items.map { { coroutine(it) } }, maxParallel)
    }


}