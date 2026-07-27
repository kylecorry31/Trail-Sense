package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class ImageTileTest {

    @Test
    fun neighborImageReadsDoNotDeadlockWhileBothTilesReload() {
        val readersReady = CountDownLatch(2)
        val loadersReady = CountDownLatch(2)
        val readNeighbors = CountDownLatch(1)
        val first = imageTile("first", loadersReady)
        val second = imageTile("second", loadersReady)

        val firstReader = daemonThread("first-reader") {
            first.withImage {
                readersReady.countDown()
                readNeighbors.await()
                second.withImage {}
            }
        }
        val secondReader = daemonThread("second-reader") {
            second.withImage {
                readersReady.countDown()
                readNeighbors.await()
                first.withImage {}
            }
        }

        firstReader.start()
        secondReader.start()
        assertTrue(readersReady.await(1, TimeUnit.SECONDS))

        val firstWriter = daemonThread("first-writer") {
            runBlocking { first.load() }
        }
        val secondWriter = daemonThread("second-writer") {
            runBlocking { second.load() }
        }

        firstWriter.start()
        secondWriter.start()
        assertTrue(loadersReady.await(1, TimeUnit.SECONDS))
        assertTrue(waitUntilWaiting(firstWriter))
        assertTrue(waitUntilWaiting(secondWriter))

        readNeighbors.countDown()

        val threads = listOf(firstReader, secondReader, firstWriter, secondWriter)
        threads.forEach { it.join(250) }
        assertFalse(
            threads.any { it.isAlive },
            "Neighbor image reads and queued reloads formed a lock cycle: " +
                    threads.joinToString { "${it.name}=${it.state}" }
        )
    }

    private fun imageTile(key: String, loadersReady: CountDownLatch): ImageTile {
        return ImageTile(
            key = key,
            tile = Tile(0, 0, 1),
            image = mock<Bitmap>(),
            loadFunction = {
                loadersReady.countDown()
                mock<Bitmap>()
            }
        )
    }

    private fun daemonThread(name: String, block: () -> Unit): Thread {
        return Thread(block, name).apply {
            isDaemon = true
        }
    }

    private fun waitUntilWaiting(thread: Thread): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (System.nanoTime() < deadline) {
            if (thread.state == Thread.State.WAITING) {
                return true
            }
            Thread.yield()
        }
        return false
    }
}
