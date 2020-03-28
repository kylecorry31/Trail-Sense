package com.kylecorry.trail_sense.sensors.altimeter

import com.kylecorry.trail_sense.models.AltitudeReading
import com.kylecorry.trail_sense.sensors.ISensor
import com.kylecorry.trail_sense.sensors.barometer.Barometer
import android.content.Context
import android.hardware.SensorManager
import java.time.Instant
import java.util.*

class BarometricAltimeter(ctx: Context): IAltimeter, ISensor, Observer, Observable() {

    private val barometer = Barometer(ctx)

    override val altitude: AltitudeReading
        get() = AltitudeReading(Instant.now(), SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, barometer.pressure.value))

    override fun start() {
        barometer.start()
        barometer.addObserver(this)
    }

    override fun stop() {
        barometer.stop()
        barometer.deleteObserver(this)
    }

    override fun update(o: Observable?, arg: Any?) {
        setChanged()
        notifyObservers()
    }
}