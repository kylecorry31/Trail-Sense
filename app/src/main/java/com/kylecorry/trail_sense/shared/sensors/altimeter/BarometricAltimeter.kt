package com.kylecorry.trail_sense.shared.sensors.altimeter

import com.kylecorry.trail_sense.shared.AltitudeReading
import com.kylecorry.trail_sense.shared.sensors.ISensor
import com.kylecorry.trail_sense.shared.sensors.barometer.Barometer
import android.content.Context
import android.hardware.SensorManager
import java.time.Instant
import java.util.*

class BarometricAltimeter(ctx: Context): IAltimeter, ISensor, Observer, Observable() {

    private val barometer = Barometer(ctx)

    private var altitudeChange = 0f
    private var lastPressure: Float? = null
    private var baseAltitude: Float? = null

    override val altitude: AltitudeReading
        get() = AltitudeReading(
            Instant.now(),
            altitudeChange + (baseAltitude ?: 0f)
        )

    override fun start() {
        altitudeChange = 0f
        barometer.start()
        barometer.addObserver(this)
    }

    override fun stop() {
        barometer.stop()
        barometer.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        if (baseAltitude == null){
            baseAltitude = getBarometricAltitude(barometer.pressure.value)
        }

        if (lastPressure != null) {
            val currentAltitude = getBarometricAltitude(barometer.pressure.value)
            val lastAltitude = getBarometricAltitude(lastPressure ?: 0f)

            altitudeChange += currentAltitude - lastAltitude
        }

        lastPressure = barometer.pressure.value

        setChanged()
        notifyObservers()
    }

    fun setAltitude(altitudeMeters: Float){
        baseAltitude = altitudeMeters
        setChanged()
        notifyObservers()
    }

    private fun getBarometricAltitude(pressure: Float): Float {
        return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)
    }


}