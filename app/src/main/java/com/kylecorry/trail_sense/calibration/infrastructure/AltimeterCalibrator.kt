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


}