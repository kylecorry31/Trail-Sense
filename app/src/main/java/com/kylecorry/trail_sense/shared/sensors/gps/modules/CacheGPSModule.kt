package com.kylecorry.trail_sense.shared.sensors.gps.modules

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.sol.time.Time.isInPast
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.sensors.gps.GPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.ModularGPSData
import com.kylecorry.trail_sense.shared.sensors.gps.SharedGPSPipeline
import java.time.Instant

data class GPSCacheData(
    val kalmanVariance: Double? = null,
    val kalmanVelocityVariance: Double? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Float = 0f,
    val bearing: Float? = null,
    val speed: Float = 0f,
    val updateTimeMillis: Long = 0L,
    val horizontalAccuracy: Float? = null,
    val verticalAccuracy: Float? = null
) : ProguardIgnore

/**
 * Persists accepted readings so the last known location survives a restart.
 */
class CacheGPSModule(
    private val cache: IPreferences = getAppService<PreferencesSubsystem>().preferences
) : GPSModule {

    override fun initialize(data: ModularGPSData): Boolean {
        if (!hasNewerReading(data)) {
            return false
        }
        restore(data)
        return true
    }

    override fun update(previousData: ModularGPSData, newData: ModularGPSData): Boolean {
        val bearing = newData.rawBearing ?: newData.bearing?.value
        val data = GPSCacheData(
            kalmanVariance = newData.kalmanVariance?.takeIf { it.isFinite() && it >= 0.0 },
            kalmanVelocityVariance = newData.kalmanVelocityVariance
                ?.takeIf { it.isFinite() && it >= 0.0 },
            latitude = newData.location.latitude,
            longitude = newData.location.longitude,
            altitude = newData.altitude,
            bearing = bearing?.takeIf { it.isFinite() },
            speed = newData.speed.value,
            updateTimeMillis = newData.time.toEpochMilli(),
            horizontalAccuracy = newData.horizontalAccuracy,
            verticalAccuracy = newData.verticalAccuracy
        )
        cache.putString(LAST_GPS, JsonConvert.toJson(data))
        return true
    }

    /**
     * The cache is written by every instance of this module, so another one may have recorded a
     * newer reading than the given data.
     */
    fun hasNewerReading(data: ModularGPSData): Boolean {
        val cacheTime = Instant.ofEpochMilli(getCachedData(cache)?.updateTimeMillis ?: 0L)
        return cacheTime > data.time && cacheTime.isInPast()
    }

    fun restore(data: ModularGPSData) {
        val cached = getCachedData(cache)
        data.kalmanVariance = cached?.kalmanVariance?.takeIf { it.isFinite() && it >= 0.0 }
        data.kalmanVelocityVariance = cached?.kalmanVelocityVariance
            ?.takeIf { it.isFinite() && it >= 0.0 }
        data.location = Coordinate(
            cached?.latitude ?: 0.0,
            cached?.longitude ?: 0.0
        )
        data.altitude = cached?.altitude ?: 0f
        data.speed =
            Speed.from(cached?.speed ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        data.time = Instant.ofEpochMilli(cached?.updateTimeMillis ?: 0L)
        data.horizontalAccuracy = cached?.horizontalAccuracy
        data.verticalAccuracy = cached?.verticalAccuracy

        data.rawBearing = cached?.bearing?.takeIf { it.isFinite() }
        data.bearing = data.rawBearing?.let { Bearing.from(it) }

        // The cache doesn't record these
        data.quality = Quality.Unknown
        data.satellites = null
        data.satelliteDetails = null
        data.mslAltitude = null
        data.bearingAccuracy = null
        data.speedAccuracy = null
        data.fixTimeElapsedNanos = null
    }

    companion object {
        const val LAST_GPS = "last_gps"

        fun clearCache() {
            SharedGPSPipeline.clearSharedCache {
                val cache = getAppService<PreferencesSubsystem>().preferences
                cache.remove(LAST_GPS)
            }
        }

        fun getCachedData(cache: IPreferences): GPSCacheData? {
            return cache.getString(LAST_GPS)?.let { JsonConvert.fromJson(it) }
        }
    }
}
