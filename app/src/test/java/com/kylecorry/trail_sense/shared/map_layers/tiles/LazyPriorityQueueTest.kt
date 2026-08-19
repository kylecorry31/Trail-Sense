package com.kylecorry.trail_sense.shared.map_layers.tiles

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class LazyPriorityQueueTest {

    private class Item(val name: String, var priority: Int)

    private fun queue(): LazyPriorityQueue<Item> {
        return LazyPriorityQueue(10, compareBy { it.priority })
    }

    @Test
    fun startsEmpty() {
        val queue = queue()

        assertEquals(0, queue.count())
        assertEquals(emptyList<Item>(), queue.dequeue())
    }

    @Test
    fun enqueuedItemsAreCounted() {
        val queue = queue()

        queue.enqueue(Item("a", 1))
        queue.enqueue(Item("b", 2))

        assertEquals(2, queue.count())
    }

    @Test
    fun dequeueReturnsHighestPriorityFirst() {
        val queue = queue()
        queue.enqueue(Item("low", 3))
        queue.enqueue(Item("high", 1))
        queue.enqueue(Item("medium", 2))

        assertEquals(listOf("high", "medium", "low"), queue.dequeue(3).map { it.name })
        assertEquals(0, queue.count())
    }

    @Test
    fun dequeueReturnsRequestedCount() {
        val queue = queue()
        queue.enqueue(Item("a", 1))
        queue.enqueue(Item("b", 2))
        queue.enqueue(Item("c", 3))

        assertEquals(listOf("a", "b"), queue.dequeue(2).map { it.name })
        assertEquals(1, queue.count())
        assertEquals(listOf("c"), queue.dequeue(2).map { it.name })
        assertEquals(0, queue.count())
    }

    @Test
    fun itemsEnqueuedAfterADequeueAreStillPrioritized() {
        val queue = queue()
        queue.enqueue(Item("b", 2))
        queue.enqueue(Item("c", 3))
        queue.dequeue()

        queue.enqueue(Item("a", 1))

        assertEquals(listOf("a", "c"), queue.dequeue(2).map { it.name })
    }

    @Test
    fun priorityChangesAreIgnoredUntilRecalculated() {
        val queue = queue()
        queue.enqueue(Item("a", 0))
        val demoted = Item("demoted", 1)
        queue.enqueue(demoted)
        queue.enqueue(Item("other", 2))
        // Move everything out of the staging queue and into the priority queue
        assertEquals(listOf("a"), queue.dequeue().map { it.name })

        demoted.priority = 100

        // The queue still believes demoted is the highest priority item
        assertEquals(listOf("demoted"), queue.dequeue().map { it.name })
    }

    @Test
    fun recalculatePrioritiesReordersExistingItems() {
        val queue = queue()
        queue.enqueue(Item("a", 0))
        val demoted = Item("demoted", 1)
        queue.enqueue(demoted)
        queue.enqueue(Item("other", 2))
        // Move everything out of the staging queue and into the priority queue
        assertEquals(listOf("a"), queue.dequeue().map { it.name })

        demoted.priority = 100
        queue.recalculatePriorities()

        assertEquals(listOf("other", "demoted"), queue.dequeue(2).map { it.name })
    }

    @Test
    fun recalculatePrioritiesOnlyAppliesToTheNextDequeue() {
        val queue = queue()
        queue.enqueue(Item("a", 0))
        val promoted = Item("promoted", 1)
        queue.enqueue(promoted)
        queue.enqueue(Item("y", 2))
        queue.enqueue(Item("z", 3))
        assertEquals(listOf("a"), queue.dequeue().map { it.name })

        promoted.priority = 100
        queue.recalculatePriorities()
        assertEquals(listOf("y"), queue.dequeue().map { it.name })

        // The flag was cleared by the last dequeue, so this change is not picked up
        promoted.priority = 0

        assertEquals(listOf("z"), queue.dequeue().map { it.name })
    }

    @Test
    fun clearRemovesStagedAndPrioritizedItems() {
        val queue = queue()
        queue.enqueue(Item("a", 1))
        queue.enqueue(Item("b", 2))
        queue.dequeue()
        queue.enqueue(Item("c", 3))

        queue.clear()

        assertEquals(0, queue.count())
        assertEquals(emptyList<Item>(), queue.dequeue(10))
    }

    @Test
    fun concurrentEnqueuesAreAllDequeued() {
        val queue = queue()
        val threads = 8
        val perThread = 200
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)

        repeat(threads) { thread ->
            pool.submit {
                start.await()
                repeat(perThread) {
                    queue.enqueue(Item("$thread-$it", thread * perThread + it))
                }
            }
        }
        start.countDown()
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.SECONDS)

        assertEquals(threads * perThread, queue.count())

        val dequeued = mutableListOf<Item>()
        while (queue.count() > 0) {
            dequeued.addAll(queue.dequeue(50))
        }

        assertEquals(threads * perThread, dequeued.size)
        assertEquals(threads * perThread, dequeued.map { it.name }.toSet().size)
        assertEquals(dequeued.map { it.priority }.sorted(), dequeued.map { it.priority })
    }
}
