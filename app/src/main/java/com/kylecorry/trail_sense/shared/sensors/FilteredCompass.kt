package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.math.filters.IFilter
import com.kylecorry.sol.units.Bearing

class FilteredCompass(private val compass: ICompass, private val filter: IFilter) :
    AbstractSensor(),
    ICompass {

    override val bearing: Bearing
        get() = Bearing(_filteredBearing)

    override var declination: Float
        get() = compass.declination
        set(value) {
            compass.declination = value
        }

    override val hasValidReading: Boolean
        get() = compass.hasValidReading

    override val rawBearing: Float
        get() = Bearing.getBearing(_filteredBearing)

    override val quality: Quality
        get() = compass.quality

    private var _filteredBearing = 0f
    private var _bearing = 0f

    override fun startImpl() {
        compass.start(this::onReading)
    }

    override fun stopImpl() {
        compass.stop(this::onReading)
    }

    private fun updateBearing(newBearing: Float) {
        _bearing += deltaAngle(_bearing, newBearing)
        _filteredBearing = filter.filter(_bearing)
    }

    private fun onReading(): Boolean {
        updateBearing(compass.rawBearing)
        notifyListeners()
        return true
    }

}