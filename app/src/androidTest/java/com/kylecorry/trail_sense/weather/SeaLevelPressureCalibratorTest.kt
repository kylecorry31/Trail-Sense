package com.kylecorry.trail_sense.weather

import android.hardware.SensorManager
import com.kylecorry.trail_sense.weather.sealevel.SeaLevelPressureCalibrator
import org.junit.Test

import org.junit.Assert.*

class SeaLevelPressureCalibratorTest {

    @Test
    fun getCalibratedPressure() {
        val pressure = 1000F
        val altitude = 200F
        assertEquals(altitude, SensorManager.getAltitude(SeaLevelPressureCalibrator.calibrate(pressure, altitude), pressure), 0.01F)
    }
}