package com.kylecorry.trail_sense.sensors.barometer

import com.kylecorry.trail_sense.models.PressureReading

interface IBarometer {
    /**
     * The pressure in hPa
     */
    val pressure: PressureReading
}