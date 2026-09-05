package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.sol.time.Time.isInPast
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import java.time.Instant

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
        cacheVariance(LAST_KALMAN_VARIANCE, newData.kalmanVariance)
        cacheVariance(LAST_KALMAN_VELOCITY_VARIANCE, newData.kalmanVelocityVariance)
        cache.putFloat(LAST_ALTITUDE, newData.altitude)
        cache.putLong(LAST_UPDATE, newData.time.toEpochMilli())
        cache.putFloat(LAST_SPEED, newData.speed.value)
        cache.putDouble(LAST_LONGITUDE, newData.location.longitude)
        cache.putDouble(LAST_LATITUDE, newData.location.latitude)
        val bearing = newData.rawBearing ?: newData.bearing?.value
        if (bearing != null && bearing.isFinite()) {
            cache.putFloat(LAST_BEARING, bearing)
        } else {
            cache.remove(LAST_BEARING)
        }
        val horizontalAccuracy = newData.horizontalAccuracy
        if (horizontalAccuracy != null) {
            cache.putFloat(LAST_HORIZONTAL_ACCURACY, horizontalAccuracy)
        } else {
            cache.remove(LAST_HORIZONTAL_ACCURACY)
        }
        val verticalAccuracy = newData.verticalAccuracy
        if (verticalAccuracy != null) {
            cache.putFloat(LAST_VERTICAL_ACCURACY, verticalAccuracy)
        } else {
            cache.remove(LAST_VERTICAL_ACCURACY)
        }
        return true
    }

    private fun cacheVariance(key: String, variance: Double?) {
        if (variance != null && variance.isFinite() && variance >= 0.0) {
            cache.putDouble(key, variance)
        } else {
            cache.remove(key)
        }
    }

    /**
     * The cache is written by every instance of this module, so another one may have recorded a
     * newer reading than the given data.
     */
    fun hasNewerReading(data: ModularGPSData): Boolean {
        val cacheTime = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        return cacheTime > data.time && cacheTime.isInPast()
    }

    fun restore(data: ModularGPSData) {
        data.kalmanVariance = cache.getDouble(LAST_KALMAN_VARIANCE)
        data.kalmanVelocityVariance = cache.getDouble(LAST_KALMAN_VELOCITY_VARIANCE)
        data.location = Coordinate(
            cache.getDouble(LAST_LATITUDE) ?: 0.0,
            cache.getDouble(LAST_LONGITUDE) ?: 0.0
        )
        data.altitude = cache.getFloat(LAST_ALTITUDE) ?: 0f
        data.speed =
            Speed.from(cache.getFloat(LAST_SPEED) ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
        data.time = Instant.ofEpochMilli(cache.getLong(LAST_UPDATE) ?: 0L)
        data.horizontalAccuracy = cache.getFloat(LAST_HORIZONTAL_ACCURACY)
        data.verticalAccuracy = cache.getFloat(LAST_VERTICAL_ACCURACY)

        data.rawBearing = cache.getFloat(LAST_BEARING)?.takeIf { it.isFinite() }
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
        const val LAST_KALMAN_VARIANCE = "last_kalman_variance"
        const val LAST_KALMAN_VELOCITY_VARIANCE = "last_kalman_velocity_variance"
        const val LAST_LATITUDE = "last_latitude_double"
        const val LAST_LONGITUDE = "last_longitude_double"
        const val LAST_ALTITUDE = "last_altitude"
        const val LAST_BEARING = "last_bearing"
        const val LAST_SPEED = "last_speed"
        const val LAST_UPDATE = "last_update"
        const val LAST_HORIZONTAL_ACCURACY = "last_horizontal_accuracy"
        const val LAST_VERTICAL_ACCURACY = "last_vertical_accuracy"

        fun clearCache() {
            SharedGPSPipeline.clearSharedCache {
                val cache = getAppService<PreferencesSubsystem>().preferences
                cache.remove(LAST_KALMAN_VARIANCE)
                cache.remove(LAST_KALMAN_VELOCITY_VARIANCE)
                cache.remove(LAST_ALTITUDE)
                cache.remove(LAST_UPDATE)
                cache.remove(LAST_SPEED)
                cache.remove(LAST_BEARING)
                cache.remove(LAST_LONGITUDE)
                cache.remove(LAST_LATITUDE)
                cache.remove(LAST_HORIZONTAL_ACCURACY)
                cache.remove(LAST_VERTICAL_ACCURACY)
            }
        }
    }
}
