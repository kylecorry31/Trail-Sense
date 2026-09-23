package com.kylecorry.trail_sense.shared.sensors.gps.modules

import android.content.Context
import android.provider.Settings
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.luna.hooks.MemoizedValue
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.GPSKalmanState
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SharedGPSPipeline
import com.kylecorry.trail_sense.shared.sensors.gps.SpeedSource
import java.time.Instant

data class GPSCacheData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Float = 0f,
    val bearing: Float? = null,
    val speed: Float = 0f,
    val updateTimeMillis: Long = 0L,
    val horizontalAccuracy: Float? = null,
    val verticalAccuracy: Float? = null,
    val kalmanState: GPSKalmanState? = null,
    val speedSource: Long? = null,
    val elapsedRealtimeNanos: Long? = null,
    val bootCount: Int? = null,
    val bootTimeMillis: Long? = null
) : ProguardIgnore

/**
 * Persists accepted readings so the last known location survives a restart.
 */
class CacheGPSModule(
    private val cache: IPreferences = getAppService<PreferencesSubsystem>().preferences,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    getBootCount: () -> Int? = { readBootCount(getAppService()) }
) : GPSModule {

    private val bootCount by lazy(getBootCount)

    override suspend fun initialize(data: ModularGPSData): Boolean {
        if (!hasNewerReading(data)) {
            return false
        }
        restore(data)
        return true
    }

    override suspend fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        val bearing = newData.rawBearing ?: newData.bearing?.value
        val data = GPSCacheData(
            latitude = newData.location.latitude,
            longitude = newData.location.longitude,
            altitude = newData.altitude,
            bearing = bearing?.takeIf { it.isFinite() },
            speed = newData.speed.value,
            updateTimeMillis = newData.eventTime.toEpochMilli(),
            horizontalAccuracy = newData.horizontalAccuracy,
            verticalAccuracy = newData.verticalAccuracy,
            kalmanState = newData.kalmanState,
            speedSource = newData.speedSource.id,
            elapsedRealtimeNanos = newData.eventTimeElapsedNanos,
            bootCount = bootCount,
            bootTimeMillis = getBootTimeMillis()
        )
        cache.putString(LAST_GPS, JsonConvert.toJson(data))
        return true
    }

    /**
     * The cache is written by every instance of this module, so another one may have recorded a
     * newer reading than the given data.
     */
    fun hasNewerReading(data: ModularGPSData): Boolean {
        val cachedElapsedNanos = getCurrentBootCachedData()?.elapsedRealtimeNanos ?: return false
        return data.location == Coordinate.zero || cachedElapsedNanos > data.eventTimeElapsedNanos
    }

    fun restore(data: ModularGPSData) {
        val cached = getCurrentBootCachedData()
        data.kalmanState = cached?.kalmanState
        data.location = Coordinate(
            cached?.latitude ?: 0.0,
            cached?.longitude ?: 0.0
        )
        data.altitude = cached?.altitude ?: 0f
        data.speed =
            Speed.from(cached?.speed ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        data.eventTime = Instant.ofEpochMilli(cached?.updateTimeMillis ?: 0L)
        data.eventTimeElapsedNanos = cached?.elapsedRealtimeNanos ?: 0L
        data.speedSource = cached?.speedSource?.let { SpeedSource.entries.withId(it) }
            ?: SpeedSource.Unknown
        data.horizontalAccuracy = cached?.horizontalAccuracy
        data.verticalAccuracy = cached?.verticalAccuracy

        data.rawBearing = cached?.bearing?.takeIf { it.isFinite() }
        data.bearing = data.rawBearing?.let { Bearing.from(it) }

        // The cache doesn't record these
        data.quality = Quality.Unknown
        data.bearingAccuracy = null
        data.speedAccuracy = null
    }

    private fun getCurrentBootCachedData(): GPSCacheData? {
        val cached = getCachedData(cache) ?: return null
        val nowElapsedNanos = getElapsedRealtimeNanos()
        val cachedElapsedNanos = cached.elapsedRealtimeNanos
        val isSameBoot = if (bootCount != null && cached.bootCount != null) {
            cached.bootCount == bootCount
        } else {
            cached.bootTimeMillis?.let {
                kotlin.math.abs(it - getBootTimeMillis()) <= BOOT_TIME_TOLERANCE_MILLIS
            } == true
        }
        val isCurrentBoot = cachedElapsedNanos != null &&
            isSameBoot &&
            cachedElapsedNanos <= nowElapsedNanos
        if (isCurrentBoot) {
            return cached
        }

        val ageMillis = timeProvider.currentTimeMillis() - cached.updateTimeMillis
        val updated = cached.copy(
            elapsedRealtimeNanos = nowElapsedNanos - ageMillis.coerceAtLeast(0L) * NANOS_PER_MILLI,
            bootCount = bootCount,
            bootTimeMillis = getBootTimeMillis()
        )
        cache.putString(LAST_GPS, JsonConvert.toJson(updated))
        return updated
    }

    private fun getElapsedRealtimeNanos(): Long {
        return timeProvider.elapsedRealtime() * NANOS_PER_MILLI
    }

    private fun getBootTimeMillis(): Long {
        return timeProvider.currentTimeMillis() - timeProvider.elapsedRealtime()
    }

    companion object {
        const val LAST_GPS = "last_gps"
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val BOOT_TIME_TOLERANCE_MILLIS = 60_000L

        private val cacheParser = MemoizedValue<GPSCacheData?>()

        suspend fun clearCache() {
            SharedGPSPipeline.clearSharedCache {
                val cache = getAppService<PreferencesSubsystem>().preferences
                cache.remove(LAST_GPS)
            }
        }

        private fun readBootCount(context: Context): Int? {
            return Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
                .takeIf { it >= 0 }
        }

        fun getCachedData(cache: IPreferences): GPSCacheData? {
            val json = cache.getString(LAST_GPS)
            return cacheParser.getOrPut(json) {
                json?.let { JsonConvert.fromJson(it) }
            }
        }
    }
}
