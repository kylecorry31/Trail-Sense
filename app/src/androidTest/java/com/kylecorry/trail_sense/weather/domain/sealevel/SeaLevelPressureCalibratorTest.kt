package com.kylecorry.trail_sense.weather.domain.sealevel

import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Test

class SeaLevelPressureCalibratorTest {

    @Test
    fun getCalibratedPressure() {
        val pressure = 1000F
        val altitude = 200F
        assertEquals(altitude, SensorManager.getAltitude(SeaLevelPressureCalibrator.calibrate(pressure, altitude), pressure), 0.01F)
    }
}