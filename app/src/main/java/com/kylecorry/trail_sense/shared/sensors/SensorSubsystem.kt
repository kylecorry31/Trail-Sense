package com.kylecorry.trail_sense.shared.sensors

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.math.SolMath.real
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.permissions.canGetLocationCustom
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.OverrideAltimeter
import java.time.Duration

class SensorSubsystem private constructor(private val context: Context) {
    private val sensorService by lazy { SensorService(context) }
    private val userPrefs by lazy { UserPreferences(context) }

    /**
     * Get the last known location without starting the GPS. May be stale.
     * @return the last known location, may be Coordinate.zero if the location is unknown
     */
    val lastKnownLocation: Coordinate
        get() {
            return sensorService.getGPS().location
        }

    /**
     * Get the last known elevation without starting the altimeter. May be stale.
     * @return the last known elevation, may be Distance.meters(0f) if elevation is unknown
     */
    val lastKnownElevation: Distance
        get() {
            val mode = userPrefs.altimeterMode
            val altimeter = when (mode) {
                UserPreferences.AltimeterMode.Override -> OverrideAltimeter(context)
                UserPreferences.AltimeterMode.Barometer, UserPreferences.AltimeterMode.GPSBarometer, UserPreferences.AltimeterMode.DigitalElevationModel, UserPreferences.AltimeterMode.DigitalElevationModelBarometer -> CachedAltimeter(
                    context
                )

                UserPreferences.AltimeterMode.GPS -> if (Permissions.canGetLocationCustom(context)) CachedAltimeter(
                    context
                ) else OverrideAltimeter(context)
            }
            return Distance.meters(altimeter.altitude.real(0f))
        }

    /**
     * Get the current location
     * @param preferredPolicy the preferred policy for getting the location
     * @param timeout the maximum time to wait for the location
     * @return the location, may be Coordinate.zero if the location is unknown
     */
    suspend fun getLocation(
        preferredPolicy: SensorRefreshPolicy = SensorRefreshPolicy.RefreshIfInvalid,
        timeout: Duration = Duration.ofSeconds(15)
    ): Coordinate {
        if (preferredPolicy == SensorRefreshPolicy.Cache) {
            return lastKnownLocation
        }

        val gps = sensorService.getGPS()

        readAll(
            listOf(gps),
            timeout,
            onlyIfInvalid = preferredPolicy == SensorRefreshPolicy.RefreshIfInvalid
        )

        return gps.location
    }

    /**
     * Get the current elevation
     * @param preferredPolicy the preferred policy for getting the elevation
     * @param timeout the maximum time to wait for the elevation
     * @return the elevation, may be Distance.meters(0f) if elevation is unknown
     */
    suspend fun getElevation(
        preferredPolicy: SensorRefreshPolicy = SensorRefreshPolicy.RefreshIfInvalid,
        timeout: Duration = Duration.ofSeconds(15)
    ): Distance {
        if (preferredPolicy == SensorRefreshPolicy.Cache) {
            return lastKnownElevation
        }

        val altimeter = sensorService.getAltimeter()

        readAll(
            listOf(altimeter),
            timeout,
            onlyIfInvalid = preferredPolicy == SensorRefreshPolicy.RefreshIfInvalid
        )

        return Distance.meters(altimeter.altitude)
    }

    enum class SensorRefreshPolicy {
        RefreshIfInvalid,
        Refresh,
        Cache
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: SensorSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): SensorSubsystem {
            if (instance == null) {
                instance = SensorSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}