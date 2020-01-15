package com.kylecorry.trail_sense.weather

import android.hardware.SensorManager
import org.junit.Test

import org.junit.Assert.*

class SeaLevelPressureCalibratorTest {

    @Test
    fun getCalibratedPressure() {
        val pressure = 1000F
        val altitude = 200F
        val pressureCalibrator = SeaLevelPressureCalibrator()

        assertEquals(altitude, SensorManager.getAltitude(pressureCalibrator.getCalibratedPressure(pressure, altitude), pressure), 0.01F)
    }
}