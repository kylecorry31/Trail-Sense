package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.data.DataUtils
import com.kylecorry.trail_sense.shared.debugging.DebugElevationsCommand
import com.kylecorry.andromeda.core.coroutines.onIO
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.OverrideAltimeter
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import java.time.Duration
import java.time.Instant

class LocationSubsystem private constructor(private val context: Context) {

    private val sensorService by lazy { SensorService(context) }
    private val altimeterCache by lazy { CachedAltimeter(context) }
    private val altimeterOverride by lazy { OverrideAltimeter(context) }
    private val weather by lazy { WeatherSubsystem.getInstance(context) }
    private val paths by lazy { PathService.getInstance(context) }

    private val maxElevationHistoryDuration = Duration.ofDays(1)
    private val maxElevationFilterHistoryDuration = maxElevationHistoryDuration.plusHours(6)

    val location: Coordinate
        get() = sensorService.getGPS().location

    val elevation: Distance
        get(){
            val raw = if (isAltimeterOverridden()) altimeterOverride.altitude else altimeterCache.altitude
            return Distance.meters(raw.real(0f))
        }

    private val userPrefs by lazy { UserPreferences(context) }

    private fun isAltimeterOverridden(): Boolean {
        val mode = userPrefs.altimeterMode
        val usesOverride = mode == UserPreferences.AltimeterMode.Override
        val usesGPS =
            mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.GPS
        return usesOverride || (usesGPS && !sensorService.hasLocationPermission())
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