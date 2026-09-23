package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.trail_sense.settings.migrations.InMemoryPreferences
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSKalmanState
import com.kylecorry.trail_sense.shared.sensors.gps.age
import com.kylecorry.trail_sense.shared.sensors.gps.durationSince
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CacheGPSModuleTest {
    private val preferences = InMemoryPreferences()
    private val fixTime = Instant.parse("2020-01-01T00:00:00Z")
    private var elapsedMillis = 110_000L
    private var wallMillis = fixTime.toEpochMilli() + 10_000L
    private val timeProvider = object : TimeProvider {
        override fun elapsedRealtime() = elapsedMillis
        override fun currentTimeMillis() = wallMillis
    }
    private val module = module(1)
    private val previous = ModularGPSData()

    private fun module(bootCount: Int?) = CacheGPSModule(preferences, timeProvider) { bootCount }

    private fun reading() = ModularGPSData(
        location = Coordinate(42.0, -72.0), altitude = 123f,
        eventTime = fixTime, eventTimeElapsedNanos = 100_000_000_000,
        speed = Speed.from(3f, DistanceUnits.Meters, TimeUnits.Seconds),
        horizontalAccuracy = 5f, verticalAccuracy = 8f
    )

    @Test
    fun preservesSpeedSourceAndTreatsLegacyCacheAsUnknown() = runBlocking<Unit> {
        val restored = ModularGPSData()
        for (source in SpeedSource.entries) {
            module.update(previous, reading().apply { speedSource = source })
            module.restore(restored)
            assertEquals(source, restored.speedSource)
        }
        preferences.putString(CacheGPSModule.LAST_GPS, """{"speed":3,"bearing":90}""")
        module.restore(restored)
        assertEquals(SpeedSource.Unknown, restored.speedSource)
        assertEquals(3f, restored.speed.value)
    }

    @Test
    fun persistsFilterStateSeparatelyFromReportedAccuracy() = runBlocking<Unit> {
        val candidate = reading().apply {
            kalmanState = GPSKalmanState(
                listOf(1f, 2f, 3f, 4f),
                List(4) { row -> List(4) { column -> if (row == column) 2f else 0f } },
                42.1,
                -72.1
            )
        }
        module.update(previous, candidate)
        val restored = ModularGPSData()
        module(1).restore(restored)
        assertEquals(candidate.kalmanState, restored.kalmanState)
        assertEquals(candidate.horizontalAccuracy, restored.horizontalAccuracy)

        // A reading accepted with smoothing disabled must clear old filter state.
        module.update(restored, reading())
        module.restore(restored)
        assertNull(restored.kalmanState)
    }

    @Test
    fun cachesBearingAndRemovesItWhenUnavailable() = runBlocking<Unit> {
        val candidate = reading().apply { rawBearing = 123f }
        module.update(previous, candidate)
        val restored = ModularGPSData()
        module(1).restore(restored)
        assertEquals(123f, restored.rawBearing)
        assertEquals(Bearing.from(123f), restored.bearing)

        module.update(previous, reading())
        module.restore(restored)
        assertNull(restored.rawBearing)
        assertNull(restored.bearing)
    }

    @Test
    fun cachesBearingWhenRawBearingIsUnavailable() = runBlocking<Unit> {
        module.update(previous, reading().apply { bearing = Bearing.from(90f) })
        val restored = ModularGPSData()
        module.restore(restored)
        assertEquals(90f, restored.rawBearing)
        assertEquals(Bearing.from(90f), restored.bearing)
    }

    @Test
    fun restoresPersistedFieldsAcrossModuleInstances() = runBlocking<Unit> {
        val candidate = reading()
        assertTrue(module.update(previous, candidate))
        val restored = ModularGPSData()
        module(1).restore(restored)
        assertEquals(candidate.location, restored.location)
        assertEquals(candidate.altitude, restored.altitude)
        assertEquals(candidate.eventTime, restored.eventTime)
        assertEquals(candidate.eventTimeElapsedNanos, restored.eventTimeElapsedNanos)
        assertEquals(candidate.speed, restored.speed)
        assertEquals(candidate.horizontalAccuracy, restored.horizontalAccuracy)
        assertEquals(candidate.verticalAccuracy, restored.verticalAccuracy)
        assertEquals(Coordinate.zero, previous.location)
    }

    @Test
    fun persistsTheCacheAsOneJsonPreference() = runBlocking<Unit> {
        val candidate = reading().apply {
            rawBearing = 123f
            kalmanState = GPSKalmanState(
                listOf(1f, 2f, 3f, 4f),
                List(4) { List(4) { 0f } },
                42.0,
                -72.0
            )
        }

        module.update(previous, candidate)

        assertTrue(preferences.contains(CacheGPSModule.LAST_GPS))
    }

    @Test
    fun missingAccuraciesRemovePreviouslyCachedValues() = runBlocking<Unit> {
        module.update(previous, reading())
        module.update(previous, reading().apply {
            horizontalAccuracy = null
            verticalAccuracy = null
        })
        val restored = reading()
        module.restore(restored)
        assertNull(restored.horizontalAccuracy)
        assertNull(restored.verticalAccuracy)
    }

    @Test
    fun restoreClearsFieldsThatAreNotPersisted() = runBlocking<Unit> {
        module.update(previous, reading())
        val restored = ModularGPSData(
            rawBearing = 20f, bearing = Bearing.from(20f), bearingAccuracy = 1f,
            speedAccuracy = 2f, eventTimeElapsedNanos = 123L
        )
        module.restore(restored)
        assertEquals(Quality.Unknown, restored.quality)
        assertNull(restored.rawBearing)
        assertNull(restored.bearing)
        assertNull(restored.bearingAccuracy)
        assertNull(restored.speedAccuracy)
    }

    @Test
    fun emptyCacheRestoresDefaults() = runBlocking<Unit> {
        val restored = reading()
        module.restore(restored)
        assertEquals(Coordinate.zero, restored.location)
        assertEquals(Instant.EPOCH, restored.eventTime)
        assertEquals(0L, restored.eventTimeElapsedNanos)
        assertEquals(0f, restored.altitude)
        assertEquals(0f, restored.speed.value)
        assertNull(restored.horizontalAccuracy)
        assertNull(restored.verticalAccuracy)
    }

    @Test
    fun onlyReadingsNewerThanCurrentDataAreRestorable() = runBlocking<Unit> {
        assertFalse(module.hasNewerReading(ModularGPSData()))
        val candidate = reading()
        module.update(previous, candidate)
        assertTrue(module.hasNewerReading(ModularGPSData()))
        assertFalse(module.hasNewerReading(candidate))
        assertFalse(module.hasNewerReading(reading().apply {
            eventTime = fixTime.minusSeconds(60)
            eventTimeElapsedNanos += 1_000_000_000
        }))
        assertTrue(module.hasNewerReading(reading().apply {
            eventTime = fixTime.plusSeconds(60)
            eventTimeElapsedNanos -= 1_000_000_000
        }))
    }

    @Test
    fun estimatesTheElapsedTimeOfAFixFromAPreviousBoot() = runBlocking<Unit> {
        module.update(previous, reading())
        elapsedMillis = 5_000L
        wallMillis = fixTime.toEpochMilli() + 60_000L

        val restored = ModularGPSData()
        module(2).restore(restored)
        assertEquals(-55_000_000_000L, restored.eventTimeElapsedNanos)
        assertEquals(Duration.ofSeconds(60), restored.age(timeProvider))

        // The estimate is saved, so it doesn't drift when read again
        wallMillis += 1
        val again = ModularGPSData()
        module(2).restore(again)
        assertEquals(restored.eventTimeElapsedNanos, again.eventTimeElapsedNanos)
        assertEquals(restored.id, again.id)

        val live = reading().apply {
            eventTime = fixTime.plusSeconds(61)
            eventTimeElapsedNanos = 6_000_000_000
        }
        assertFalse(module(2).hasNewerReading(live))
        assertEquals(Duration.ofSeconds(61), live.durationSince(restored))
    }

    @Test
    fun detectsARebootFromTheElapsedTimeWhenTheBootCountIsUnavailable() = runBlocking<Unit> {
        module(null).update(previous, reading())
        val sameBoot = ModularGPSData()
        module(null).restore(sameBoot)
        assertEquals(100_000_000_000L, sameBoot.eventTimeElapsedNanos)

        // The new boot has a longer uptime than the old fix, so elapsed-time rollback alone
        // cannot identify the reboot.
        elapsedMillis = 200_000L
        wallMillis += 10_000L
        val rebooted = ModularGPSData()
        module(null).restore(rebooted)
        assertEquals(180_000_000_000L, rebooted.eventTimeElapsedNanos)
    }

    @Test
    fun estimatesTheElapsedTimeOfALegacyCache() = runBlocking<Unit> {
        preferences.putString(
            CacheGPSModule.LAST_GPS,
            JsonConvert.toJson(GPSCacheData(latitude = 1.0, longitude = 2.0, updateTimeMillis = fixTime.toEpochMilli()))
        )
        val restored = ModularGPSData()
        module.restore(restored)
        assertEquals(Duration.ofSeconds(10), restored.age(timeProvider))
    }

    @Test
    fun aFixFromTheFutureIsRestoredAsCurrent() = runBlocking<Unit> {
        module.update(previous, reading())
        elapsedMillis = 5_000L
        wallMillis = fixTime.toEpochMilli() - 60_000L
        val restored = ModularGPSData()
        module(2).restore(restored)
        assertEquals(Duration.ZERO, restored.age(timeProvider))
    }
}
