package com.kylecorry.trail_sense.shared.map_layers.tiles

import java.util.PriorityQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LazyPriorityQueue<T : Any>(initialCapacity: Int, comparator: Comparator<T>) {
    private val stagingQueue = ConcurrentLinkedQueue<T>()
    private val priorityQueue = PriorityQueue(initialCapacity, comparator)
    private val count = AtomicInteger(0)
    private val stagedCount = AtomicInteger(0)
    private val priorityQueueLock = ReentrantLock()

    @Volatile
    private var prioritiesChanged = false

    fun enqueue(item: T) {
        stagingQueue.add(item)
        count.incrementAndGet()
        stagedCount.incrementAndGet()
    }

    fun recalculatePriorities() {
        prioritiesChanged = true
    }

    fun dequeue(count: Int = 1): List<T> = priorityQueueLock.withLock {
        val stagedItems = (0 until stagedCount.get()).mapNotNull {
            val item = stagingQueue.poll()
            if (item != null) {
                stagedCount.decrementAndGet()
            }
            item
        }

        if (prioritiesChanged) {
            // WHY: Clear the flag first so changes made during the rebuild are picked up on the next dequeue
            prioritiesChanged = false
            val originalItems = priorityQueue.toList()
            priorityQueue.clear()
            priorityQueue.addAll(originalItems)
        }

        priorityQueue.addAll(stagedItems)

        return (0 until count).mapNotNull {
            val item = priorityQueue.poll()
            if (item != null) {
                this.count.decrementAndGet()
            }
            item
        }
    }

    fun clear() = priorityQueueLock.withLock {
        stagingQueue.clear()
        priorityQueue.clear()
        count.set(0)
        stagedCount.set(0)
        prioritiesChanged = false
    }

    fun count(): Int {
        return count.get()
    }

}
