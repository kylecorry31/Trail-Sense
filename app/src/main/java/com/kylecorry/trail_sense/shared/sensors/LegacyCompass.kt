package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.weather.domain.MovingAverageFilter
import kotlin.math.abs
import kotlin.math.floor

class LegacyCompass(context: Context): BaseSensor(context, Sensor.TYPE_ORIENTATION, SensorManager.SENSOR_DELAY_FASTEST), ICompass {

    private val prefs = UserPreferences(context)
    private var filterSize = prefs.navigation.compassSmoothing * 2
    private val filter = MovingAverageFilter(filterSize)

    override var declination = 0f

    override val bearing: Bearing
        get() = Bearing(_filteredBearing).withDeclination(declination)

    private var _bearing = 0f
    private var _filteredBearing = 0f

    override fun setSmoothing(smoothing: Int) {
        filterSize = smoothing * 2
        filter.size = filterSize
    }

    override fun handleSensorEvent(event: SensorEvent) {
        _bearing += deltaAngle(_bearing, event.values[0])

        _filteredBearing = filter.filter(_bearing.toDouble()).toFloat()
    }

    private fun deltaAngle(angle1: Float, angle2: Float): Float {
        var delta = angle2 - angle1
        delta += 180
        delta -= floor(delta / 360) * 360
        delta -= 180
        if (abs(abs(delta) - 180) <= Float.MIN_VALUE){
            delta = 180f
        }
        return delta
    }

}