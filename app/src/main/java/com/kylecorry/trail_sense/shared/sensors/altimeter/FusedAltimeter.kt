package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.sol.math.filters.IFilter
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure

class FusedAltimeter(
    private val gps: IGPS,
    private val barometer: IBarometer,
    gpsAltitudeSampleCount: Int
) : AbstractSensor(), IAltimeter {

    private val gpsAltimeter = GaussianAltimeterWrapper(gps, gpsAltitudeSampleCount)

    override val altitude: Float
        get() {
            val seaLevelPressure = seaLevelPressure ?: return gpsAltimeter.altitude

            return Geology.getAltitude(Pressure.hpa(filteredPressure), seaLevelPressure).distance
        }

    override val hasValidReading: Boolean
        get() = seaLevelPressure != null

    private var seaLevelPressure: Pressure? = null
    private var pressureFilter: IFilter? = null
    private var filteredPressure = barometer.pressure

    override fun startImpl() {
        seaLevelPressure = null
        pressureFilter = null
        trySetSeaLevelPressure()
        gpsAltimeter.start(this::onBaseAltimeterUpdate)
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        gpsAltimeter.stop(this::onBaseAltimeterUpdate)
        barometer.stop(this::onBarometerUpdate)
    }

    private fun onBarometerUpdate(): Boolean {
        trySetSeaLevelPressure()
        updatePressure(barometer.pressure)
        if (seaLevelPressure != null){
            notifyListeners()
        }
        return true
    }

    private fun onBaseAltimeterUpdate(): Boolean {
        trySetSeaLevelPressure()
        if (seaLevelPressure != null){
            notifyListeners()
        }
        return seaLevelPressure == null
    }

    private fun trySetSeaLevelPressure() {
        if (seaLevelPressure == null && barometer.hasValidReading && gpsAltimeter.hasValidReading) {
            seaLevelPressure = Meteorology.getSeaLevelPressure(
                Pressure.hpa(barometer.pressure), Distance.meters(gpsAltimeter.altitude)
            )
        }
    }

    private fun updatePressure(pressure: Float){
        val filter = pressureFilter ?: LowPassFilter(0.2f, barometer.pressure)
        pressureFilter = filter
        filteredPressure = filter.filter(pressure)
    }
}