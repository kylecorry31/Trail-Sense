package com.kylecorry.trail_sense.calibration.infrastructure

import android.content.Context
import android.hardware.SensorManager
import com.kylecorry.trail_sense.shared.UserPreferences

class AltimeterCalibrator(context: Context) {

    private val prefs = UserPreferences(context)

    fun setManualElevation(elevation: Float){
        prefs.useAutoAltitude = false
        prefs.altitudeOverride = elevation
    }

    fun getManualElevation(): Float {
        return prefs.altitudeOverride
    }

    fun setManualElevationFromPressure(pressure: Float, seaLevelPressure: Float){
        val altitude = SensorManager.getAltitude(seaLevelPressure, pressure)
        setManualElevation(altitude)
    }

    fun setElevationAuto(){
        prefs.useAutoAltitude = true
    }

    fun isElevationAuto(): Boolean {
        return prefs.useAutoAltitude
    }

    fun setFineTuneWithBarometer(fineTune: Boolean){

    }

    fun isFineTuningWithBarometer(): Boolean {
        return true
    }

    fun setUseElevationOffsets(useOffsets: Boolean){
        prefs.useAltitudeOffsets = useOffsets
    }

    fun isUsingElevationOffsets(): Boolean {
        return prefs.useAltitudeOffsets
    }



}