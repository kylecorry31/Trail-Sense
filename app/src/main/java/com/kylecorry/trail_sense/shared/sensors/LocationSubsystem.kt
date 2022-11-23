package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.navigation.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant

class LocationSubsystem private constructor(private val context: Context) {

    private val gpsCache by lazy { CachedGPS(context) }
    private val gpsOverride by lazy { OverrideGPS(context) }
    private val altimeterCache by lazy { CachedAltimeter(context) }
    private val altimeterOverride by lazy { OverrideAltimeter(context) }
    private val weather by lazy { WeatherSubsystem.getInstance(context) }
    private val paths by lazy { PathService.getInstance(context) }

    private val maxElevationHistoryDuration = Duration.ofDays(1)
    private val maxElevationFilterHistoryDuration = maxElevationHistoryDuration.plusHours(6)

    val location: Coordinate
        get() = if (isGPSOverridden()) gpsOverride.location else gpsCache.location

    val elevation: Distance
        get() = Distance.meters(if (isAltimeterOverridden()) altimeterOverride.altitude else altimeterCache.altitude)

    private val userPrefs by lazy { UserPreferences(context) }

    suspend fun getRawElevationHistory(): List<Reading<Float>> {
        val fromWeather = weather.getRawHistory().map { Reading(it.value.altitude, it.time) }
        val backtrack = paths.getRecentAltitudes(
            Instant.now().minus(maxElevationFilterHistoryDuration)
        )
        // TODO: Add current altitude
        val readings = (fromWeather + backtrack)
            .sortedBy { it.time }
            .filter {
                Duration.between(
                    it.time,
                    Instant.now()
                ) < maxElevationHistoryDuration
            }

        return readings
    }

    suspend fun getElevationHistory(): List<Reading<Float>> {
        val fromWeather = weather.getRawHistory().map { Reading(it.value.altitude, it.time) }
        val backtrack = paths.getRecentAltitudes(
            Instant.now().minus(maxElevationFilterHistoryDuration)
        )
        // TODO: Add current altitude
        val readings = (fromWeather + backtrack)
            .sortedBy { it.time }
            .filter {
                Duration.between(
                    it.time,
                    Instant.now()
                ) < maxElevationFilterHistoryDuration
            }

        val smoothed = DataUtils.smoothTemporal(readings, 0.3f)

        onIO {
            DebugElevationsCommand(context, readings, smoothed).execute()
        }

        return smoothed.filter {
            Duration.between(it.time, Instant.now()).abs() <= maxElevationHistoryDuration
        }
    }

    private fun isAltimeterOverridden(): Boolean {
        val mode = userPrefs.altimeterMode
        val usesOverride = mode == UserPreferences.AltimeterMode.Override
        val usesGPS =
            mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.GPS
        if (usesOverride || (usesGPS && !Permissions.canGetFineLocation(context))) {
            return true
        }

        return false
    }

    private fun isGPSOverridden(): Boolean {
        if (!userPrefs.useAutoLocation || !Permissions.canGetFineLocation(context)) {
            return true
        }

        return false
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