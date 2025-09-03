package com.kylecorry.trail_sense.shared.canvas

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

class CoroutineQueueRunner2(
    private val queueSize: Int = 1,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val dispatcher: CoroutineContext = Dispatchers.Default,
    private val ignoreExceptions: Boolean = false,
    private val queuePolicy: BufferOverflow = BufferOverflow.DROP_LATEST
) {
    private var taskChannel = Channel<suspend () -> Unit>(queueSize, queuePolicy)
    private var consumerJob: Job? = null
    private var isRunningTask = false
    private val mutex = Mutex()
    private val replaceMutex = Mutex()

    init {
        launchConsumer()
    }

    private fun launchConsumer() {
        consumerJob?.cancel() // cancel the existing consumer job
        consumerJob = scope.launch {
            for (task in taskChannel) {
                try {
                    mutex.withLock { isRunningTask = true }
                    withContext(dispatcher) {
                        task.invoke()
                    }
                } catch (e: Exception) {
                    if (!ignoreExceptions) {
                        throw e
                    }
                } finally {
                    mutex.withLock { isRunningTask = false }
                }
            }
        }
    }

    suspend fun enqueue(task: suspend () -> Unit): Boolean {
        checkConsumer()
        val result = taskChannel.trySend(task)
        return result.isSuccess
    }

    suspend fun replace(task: suspend () -> Unit) {
        replaceMutex.withLock {
            cancelAndJoin()
            enqueue(task)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun skipIfRunning(task: suspend () -> Unit): Boolean {
        checkConsumer()
        val shouldEnqueue = mutex.withLock { !isRunningTask && taskChannel.isEmpty }
        if (shouldEnqueue) {
            enqueue(task)
        }
        return shouldEnqueue
    }

    suspend fun cancelAndJoin() {
        consumerJob?.cancelAndJoin()
        taskChannel.close()
    }

    fun cancel() {
        consumerJob?.cancel()
        taskChannel.close()
    }

    private fun checkConsumer() {
        if (consumerJob?.isActive != true) {
            taskChannel = Channel(queueSize)
            launchConsumer()
        }
    }
}
