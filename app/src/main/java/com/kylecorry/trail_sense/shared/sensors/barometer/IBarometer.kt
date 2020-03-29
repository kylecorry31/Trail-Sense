package com.kylecorry.trail_sense.shared.sensors.barometer

import com.kylecorry.trail_sense.shared.PressureReading

interface IBarometer {
    /**
     * The pressure in hPa
     */
    val pressure: PressureReading
}