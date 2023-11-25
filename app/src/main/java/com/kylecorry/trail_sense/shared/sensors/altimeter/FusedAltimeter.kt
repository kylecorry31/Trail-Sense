package com.kylecorry.trail_sense.shared.sensors.altimeter

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.coroutines.onMain
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.sol.math.filters.IFilter
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.asFlowable
import com.kylecorry.trail_sense.shared.sensors.readAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class FusedAltimeter(
    context: Context,
    gps: IGPS,
    private val barometer: IBarometer,
) : AbstractSensor(), IAltimeter {

    private val prefs = UserPreferences(context)
    private val cache = PreferencesSubsystem.getInstance(context).preferences
    private val gpsAltimeter = GaussianAltimeterWrapper(gps, prefs.altimeterSamples)
    private val barometerFlow = barometer.asFlowable().flow

    override val altitude: Float
        get() {
            val seaLevelPressure = seaLevelPressure ?: return gpsAltimeter.altitude

            if (filteredPressure == 0f){
                return gpsAltimeter.altitude
            }

            return Geology.getAltitude(Pressure.hpa(filteredPressure), seaLevelPressure).distance
        }

    override val hasValidReading: Boolean
        get() = seaLevelPressure != null

    private var seaLevelPressure: Pressure? = null
    private var pressureFilter: IFilter? = null
    private var filteredPressure = barometer.pressure

    private var lastSeaLevelPressureTime: Instant? = null

    private val scope = CoroutineScope(Dispatchers.Default)
    private val runner = CoroutineQueueRunner()

    override fun startImpl() {
        scope.launch {
            runner.replace {
                pressureFilter = null
                calibrate()
                barometerFlow.collect {
                    updatePressure(barometer.pressure)
                    onMain {
                        notifyListeners()
                    }

                    if (!isSeaLevelPressureValid()) {
                        calibrate()
                    }
                }
            }
        }
    }

    override fun stopImpl() {
        runner.cancel()
    }

    private fun isSeaLevelPressureValid(): Boolean {
        val delta = lastSeaLevelPressureTime?.let {
            Duration.between(lastSeaLevelPressureTime, Instant.now())
        }
        return seaLevelPressure != null && delta != null && delta > Duration.ZERO && delta < CALIBRATION_INTERVAL
    }

    private fun getLastSeaLevelPressure(): Pressure? {
        return cache.getFloat(LAST_SEA_LEVEL_PRESSURE_KEY)?.let { Pressure.hpa(it) }
    }

    private fun getLastSeaLevelPressureTime(): Instant? {
        return cache.getInstant(LAST_SEA_LEVEL_PRESSURE_TIME_KEY)
    }

    private fun setLastSeaLevelPressure(pressure: Float) {
        val time = Instant.now()
        cache.putFloat(LAST_SEA_LEVEL_PRESSURE_KEY, pressure)
        cache.putInstant(LAST_SEA_LEVEL_PRESSURE_TIME_KEY, time)
        seaLevelPressure = Pressure.hpa(pressure)
        lastSeaLevelPressureTime = time
    }

    private fun updatePressure(pressure: Float) {
        val filter = pressureFilter ?: LowPassFilter(1 - SMOOTHING, barometer.pressure)
        pressureFilter = filter
        filteredPressure = filter.filter(pressure)
    }

    private suspend fun calibrate() {
        seaLevelPressure = getLastSeaLevelPressure()
        lastSeaLevelPressureTime = getLastSeaLevelPressureTime()
        if (isSeaLevelPressureValid()) {
            Log.d("FusedAltimeter", "Used cached calibration")
            return
        }

        Log.d("FusedAltimeter", "Calibrating")

        // Get a reading from the GPS and barometer
        readAll(listOf(gpsAltimeter, barometer), CALIBRATION_TIMEOUT)

        // Update the sea level pressure
        val pressure = Meteorology.getSeaLevelPressure(
            Pressure.hpa(barometer.pressure), Distance.meters(gpsAltimeter.altitude)
        )
        setLastSeaLevelPressure(pressure.pressure)
        Log.d("FusedAltimeter", "Updated calibration")
    }

    companion object {
        private const val LAST_SEA_LEVEL_PRESSURE_KEY =
            "cache_fused_altimeter_last_sea_level_pressure"
        private const val LAST_SEA_LEVEL_PRESSURE_TIME_KEY =
            "cache_fused_altimeter_last_sea_level_pressure_time"
        private const val SMOOTHING = 0.8f
        private val CALIBRATION_TIMEOUT = Duration.ofSeconds(10)
        private val CALIBRATION_INTERVAL = Duration.ofHours(1)
    }

}