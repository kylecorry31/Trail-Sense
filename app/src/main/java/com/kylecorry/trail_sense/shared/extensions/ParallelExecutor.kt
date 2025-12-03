package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.luna.coroutines.ParallelCoroutineRunner
import com.kylecorry.sol.shared.Executor
import kotlinx.coroutines.runBlocking

class ParallelExecutor(private val runner: ParallelCoroutineRunner) : Executor {
    override fun <T> map(tasks: List<() -> T>): List<T> = runBlocking {
        runner.mapFunctions(tasks)
    }

    override fun run(tasks: List<() -> Unit>) = runBlocking {
        runner.run(tasks.map { { it() } })
    }
}