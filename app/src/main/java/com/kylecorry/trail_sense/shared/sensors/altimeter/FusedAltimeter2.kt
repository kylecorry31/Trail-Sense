package com.kylecorry.trail_sense.shared.sensors.altimeter

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.coroutines.onDefault
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.sol.math.filters.ComplementaryFilter
import com.kylecorry.sol.math.filters.IFilter
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import java.time.Duration
import java.time.Instant

class FusedAltimeter2(
    context: Context,
    gps: IGPS,
    private val barometer: IBarometer,
) : AbstractSensor(), IAltimeter {

    private val prefs = UserPreferences(context)
    private val cache = PreferencesSubsystem.getInstance(context).preferences
    private val gpsAltimeter = GaussianAltimeterWrapper(gps, prefs.altimeterSamples)

    private val updateTimer = CoroutineTimer {
        val success = onDefault {
            update()
        }
        if (success) {
            notifyListeners()
        }
    }

    private var filter: ComplementaryFilter? = null

    override val altitude: Float
        get() = filter?.value ?: gpsAltimeter.altitude

    override val hasValidReading: Boolean
        get() = filter != null

    private var pressureFilter: IFilter? = null
    private var filteredPressure = barometer.pressure

    override fun startImpl() {
        pressureFilter = null
        filter = null
        filteredPressure = 0f
        gpsAltimeter.start(this::onGPSUpdate)
        barometer.start(this::onBarometerUpdate)
        updateTimer.interval(Duration.ofMillis(200))
    }

    override fun stopImpl() {
        gpsAltimeter.stop(this::onGPSUpdate)
        barometer.stop(this::onBarometerUpdate)
        updateTimer.stop()
    }

    private fun onBarometerUpdate(): Boolean {
        updatePressure(barometer.pressure)
        return true
    }

    private fun onGPSUpdate(): Boolean {
        return true
    }

    private fun updatePressure(pressure: Float) {
        val filter = pressureFilter ?: LowPassFilter(0.1f, barometer.pressure)
        pressureFilter = filter
        filteredPressure = filter.filter(pressure)
    }


    private fun update(): Boolean {
        if (filteredPressure == 0f) {
            // No barometer reading yet
            return false
        }

        val seaLevel = getLastSeaLevelPressure()

        // Sea level pressure is unknown
        if (seaLevel == null) {
            // Calibrate using the GPS if available
            if (gpsAltimeter.hasValidReading) {
                setLastSeaLevelPressure(
                    Meteorology.getSeaLevelPressure(
                        Pressure.hpa(filteredPressure),
                        Distance.meters(gpsAltimeter.altitude)
                    )
                )
            }

            return false
        }

        // Calculate the barometric altitude
        val barometricAltitude =
            Geology.getAltitude(Pressure.hpa(filteredPressure), seaLevel).distance

        // At this point, the barometric altitude is available but the GPS altitude may not be
        // If the GPS altitude is not available, use the barometric altitude instead
        val gpsAltitude = if (gpsAltimeter.hasValidReading){
            gpsAltimeter.altitude
        } else {
            barometricAltitude
        }

        // Create the filter if it doesn't exist, the value will be overwritten so it doesn't matter
        val filter = filter ?: ComplementaryFilter(listOf(ALPHA, 1 - ALPHA), gpsAltitude)
        this.filter = filter
        filter.filter(listOf(gpsAltitude, barometricAltitude))

        // Update the current estimate of the sea level pressure
        setLastSeaLevelPressure(
            Meteorology.getSeaLevelPressure(
                Pressure.hpa(filteredPressure),
                Distance.meters(filter.value)
            )
        )

//        Log.d("FusedAltimeter", "Altitude: ${filter.value}m, GPS: ${gpsAltimeter.altitude}m, Barometer: ${barometricAltitude}m, Sea level: ${seaLevel.pressure}hPa")
        return true
    }

    private fun getLastSeaLevelPressure(): Pressure? {
        val time = cache.getInstant(LAST_SEA_LEVEL_PRESSURE_TIME_KEY) ?: return null

        val timeSinceReading = Duration.between(time, Instant.now())
        if (timeSinceReading > SEA_LEVEL_EXPIRATION || timeSinceReading.isNegative) {
            // Sea level pressure has expired
            return null
        }

        return cache.getFloat(LAST_SEA_LEVEL_PRESSURE_KEY)?.let { Pressure.hpa(it) }
    }


    private fun setLastSeaLevelPressure(pressure: Pressure) {
        cache.putFloat(LAST_SEA_LEVEL_PRESSURE_KEY, pressure.pressure)

        // Only update the time if the GPS was used - this prevents only using the barometer over the long term
        if (gpsAltimeter.hasValidReading) {
            cache.putInstant(LAST_SEA_LEVEL_PRESSURE_TIME_KEY, Instant.now())
        }
    }

    companion object {
        private const val LAST_SEA_LEVEL_PRESSURE_KEY =
            "cache_fused_altimeter_last_sea_level_pressure"
        private const val LAST_SEA_LEVEL_PRESSURE_TIME_KEY =
            "cache_fused_altimeter_last_sea_level_pressure_time"

        // The amount of time before the sea level pressure expires if not updated using the GPS
        private val SEA_LEVEL_EXPIRATION = Duration.ofMinutes(1)

        // 2% GPS, 98% barometer
        private const val ALPHA = 0.02f
    }
}
