package com.kylecorry.trail_sense.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ParallelCoroutineRunner(maxParallel: Int) {

    private val semaphore = Semaphore(maxParallel)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    suspend fun run(coroutines: List<suspend () -> Unit>) {
        val jobs = mutableListOf<Job>()

        for (coroutine in coroutines) {
            jobs.add(coroutineScope.launch {
                semaphore.withPermit { coroutine() }
            })
        }

        jobs.forEach { it.join() }
    }

}