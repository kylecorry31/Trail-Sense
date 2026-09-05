package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Instant

class GPSPipelineTest {
    private val preferences = InMemoryPreferences()

    private fun pipeline(vararg modules: GPSModule) =
        GPSPipeline(modules.toList(), CacheGPSModule(preferences))

    private fun reading(seconds: Long, longitude: Double = 1.0) = ModularGPSData(
        location = Coordinate(1.0, longitude),
        time = Instant.EPOCH.plusSeconds(seconds),
        horizontalAccuracy = 10f,
        hasValidReading = true
    )

    private fun module(action: (ModularGPSData, ModularGPSData) -> Boolean) = object : GPSModule {
        override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
            return action(previousData, newData)
        }
    }

    @Test
    fun processesModulesInOrderAndCachesFinalReadingWithoutChangingSource() {
        val pipeline = pipeline(
            module { _, next -> next.altitude += 10f; true },
            module { previous, next ->
                assertEquals(Coordinate.zero, previous.location)
                next.altitude *= 2f
                true
            }
        )
        val source = reading(1).apply { altitude = 5f }
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(source))
        assertEquals(30f, pipeline.reading.altitude)
        assertEquals(5f, source.altitude)
        assertEquals(30f, pipeline().reading.altitude)
        assertTrue(pipeline.hadValidReading)
    }

    @Test
    fun rejectionPreservesAcceptedReadingAndCacheAndSkipsLaterModules() {
        var reject = false
        var laterCalls = 0
        val pipeline = pipeline(
            module { _, next -> next.altitude = 25f; !reject },
            module { _, _ -> laterCalls++; true }
        )
        pipeline.update(reading(1))
        reject = true
        assertEquals(GPSUpdateResult.Rejected, pipeline.update(reading(2, 2.0)))
        assertEquals(1, laterCalls)
        assertEquals(reading(1).location, pipeline.reading.location)
        assertEquals(reading(1).time, pipeline.reading.time)
        assertEquals(reading(1).time, pipeline().reading.time)

        reject = false
        assertEquals(GPSUpdateResult.NewFixAccepted, pipeline.update(reading(3)))
    }

    @Test
    fun sameFixUpdatesSecondaryFieldsAndHasAnExplicitResult() {
        val pipeline = pipeline()
        pipeline.update(reading(1))
        val duplicate = reading(1).apply { satellites = 8; altitude = 20f }
        assertEquals(GPSUpdateResult.SameFixUpdated, pipeline.update(duplicate))
        assertEquals(8, pipeline.reading.satellites)
        assertEquals(20f, pipeline.reading.altitude)
        assertEquals(20f, pipeline().reading.altitude)
    }

    @Test
    fun restoresNewerCacheAndResynchronizesKalmanAcrossRestarts() {
        val first = pipeline(KalmanGPSModule(mock()))
        first.start()
        first.update(reading(1))
        first.stop()

        val second = pipeline(KalmanGPSModule(mock()))
        second.update(reading(2, 1.001))
        second.update(reading(3, 1.002))
        assertTrue(first.restoreNewerCachedReading())
        assertEquals(second.reading.location, first.reading.location)
        assertFalse(first.restoreNewerCachedReading())

        first.start()
        assertEquals(GPSUpdateResult.SameFixUpdated, first.update(reading(3, 1.002)))
        assertEquals(second.reading.location, first.reading.location)
        first.update(reading(4, 1.003))
        second.update(reading(4, 1.003))
        assertEquals(second.reading.location.latitude, first.reading.location.latitude, 0.0000001)
        assertEquals(second.reading.location.longitude, first.reading.location.longitude, 0.0000001)
        assertEquals(second.reading.horizontalAccuracy!!, first.reading.horizontalAccuracy!!, 0.00001f)
    }

    @Test
    fun forwardsLifecycleWithAcceptedReading() {
        val events = mutableListOf<String>()
        val pipeline = pipeline(object : GPSModule {
            override fun update(previousData: ModularGPSData, newData: ModularGPSData) = true

            override fun start(data: ModularGPSData) {
                events.add("start:${data.time.epochSecond}")
            }

            override fun stop(data: ModularGPSData) {
                events.add("stop:${data.time.epochSecond}")
            }
        })
        pipeline.start()
        pipeline.update(reading(1))
        pipeline.stop()
        pipeline.start()
        assertEquals(listOf("start:0", "stop:1", "start:1"), events)
    }
}
