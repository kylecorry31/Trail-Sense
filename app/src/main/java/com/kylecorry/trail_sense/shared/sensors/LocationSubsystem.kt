package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachingAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.OverrideAltimeter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant

class LocationSubsystem private constructor(private val context: Context) {

    private val sensorService by lazy { SensorService(context) }
    private val altimeterOverride by lazy { OverrideAltimeter(context) }
    private val weather by lazy { WeatherSubsystem.getInstance(context) }
    private val paths by lazy { PathService.getInstance(context) }
    private val sensorSubsystem by lazy { SensorSubsystem.getInstance(context) }

    private val maxElevationHistoryDuration = Duration.ofDays(1)
    private val maxElevationFilterHistoryDuration = maxElevationHistoryDuration.plusHours(6)

    private val prefs by lazy { PreferencesSubsystem.getInstance(context).preferences }

    val location: Coordinate
        get() = sensorSubsystem.lastKnownLocation

    val locationAge: Duration
        get() {
            if (isGPSOverridden()) {
                return Duration.ZERO
            }

            val lastUpdate = Instant.ofEpochMilli(prefs.getLong(CustomGPS.LAST_UPDATE) ?: 0)
            return Duration.between(lastUpdate, Instant.now())
        }

    val elevation: Distance
        get() = sensorSubsystem.lastKnownElevation

    val elevationAge: Duration
        get() {
            if (isAltimeterOverridden()) {
                return Duration.ZERO
            }

            val lastUpdate =
                prefs.getInstant(CachingAltimeterWrapper.LAST_UPDATE_KEY) ?: Instant.EPOCH
            return Duration.between(lastUpdate, Instant.now())
        }

    init {
        val locationChangedPrefs = listOf(
            CustomGPS.LAST_LATITUDE,
            CustomGPS.LAST_LONGITUDE,
            context.getString(R.string.pref_latitude_override),
            context.getString(R.string.pref_longitude_override),
            context.getString(R.string.pref_auto_location),
            // TODO: Technically not true - this should probably by another broadcast
            context.getString(R.string.pref_coordinate_format)
        )
        val elevationChangedPrefs = listOf(
            CachingAltimeterWrapper.LAST_UPDATE_KEY,
            context.getString(R.string.pref_altitude_override),
            context.getString(R.string.pref_altimeter_calibration_mode)
        )
        prefs.onChange.subscribe { key ->
            if (locationChangedPrefs.contains(key)) {
                Tools.broadcast(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)
            }
            if (elevationChangedPrefs.contains(key)) {
                Tools.broadcast(SensorsToolRegistration.BROADCAST_ELEVATION_CHANGED)
            }
            true
        }
    }

    suspend fun updateLocation() {
        sensorSubsystem.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
    }

    private val userPrefs by lazy { UserPreferences(context) }

    private fun isAltimeterOverridden(): Boolean {
        val mode = userPrefs.altimeterMode
        val usesOverride = mode == UserPreferences.AltimeterMode.Override
        val usesGPS =
            mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.GPS
        return usesOverride || (usesGPS && !sensorService.hasLocationPermission())
    }

    private fun isGPSOverridden(): Boolean {
        return !userPrefs.useAutoLocation || !sensorService.hasLocationPermission()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: LocationSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): LocationSubsystem {
            if (instance == null) {
                instance = LocationSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}